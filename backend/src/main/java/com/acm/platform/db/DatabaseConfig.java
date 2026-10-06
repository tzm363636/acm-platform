package com.acm.platform.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.Properties;

@Configuration
@Profile({"mysql-local", "aiven"})
public class DatabaseConfig {
    @Bean(destroyMethod="close")
    HikariDataSource dataSource(Environment env) {
        String host = env.getRequiredProperty("acm.db.host");
        String port = env.getRequiredProperty("acm.db.port");
        String name = env.getRequiredProperty("acm.db.name");
        validateTarget(host,port,name);
        String mode = env.getRequiredProperty("acm.db.ssl-mode");
        boolean cloud = env.acceptsProfiles(org.springframework.core.env.Profiles.of("aiven"));
        if (cloud && !"VERIFY_IDENTITY".equals(mode)) throw new IllegalArgumentException("Cloud TLS identity verification is required.");
        if (!mode.equals("VERIFY_IDENTITY") && !(mode.equals("DISABLED") && !cloud && (host.equals("127.0.0.1") || host.equals("localhost") || host.equals("::1"))))
            throw new IllegalArgumentException("TLS must verify identity; unencrypted transport is allowed only for explicitly configured loopback development.");
        var config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://"+(host.contains(":")?"["+host+"]":host)+":"+port+"/"+name);
        config.setUsername(env.getRequiredProperty("acm.db.username"));
        config.setPassword(env.getRequiredProperty("acm.db.password"));
        config.setMaximumPoolSize(env.getRequiredProperty("acm.db.pool-max",Integer.class));
        config.setMinimumIdle(0); config.setConnectionTimeout(env.getProperty("acm.db.pool-timeout-ms",Long.class,30000L));
        // Cloud/network idle connections can expire before MySQL's own wait_timeout.
        config.setMaxLifetime(env.getProperty("acm.db.pool-max-lifetime-ms",Long.class,120000L));
        config.setIdleTimeout(env.getProperty("acm.db.pool-idle-timeout-ms",Long.class,60000L));
        config.setKeepaliveTime(env.getProperty("acm.db.pool-keepalive-ms",Long.class,30000L));
        config.setInitializationFailTimeout(-1); config.setPoolName("acm-mysql");
        config.setConnectionInitSql("SET time_zone = '+00:00'");
        Properties props = connectionProperties(env,mode);
        config.setDataSourceProperties(props);
        return new HikariDataSource(config);
    }
    static void validateTarget(String host,String port,String name) {
        if (!host.matches("[a-zA-Z0-9.:-]+") || !name.matches("[a-zA-Z0-9_]+")) throw new IllegalArgumentException("Invalid database target fields.");
        try { int n=Integer.parseInt(port); if(n<1||n>65535) throw new NumberFormatException(); }
        catch(NumberFormatException e){ throw new IllegalArgumentException("Invalid database port."); }
    }
    static Properties connectionProperties(Environment env,String mode) {
        var p=new Properties();
        p.setProperty("sslMode",mode); p.setProperty("paranoid","true");
        p.setProperty("characterEncoding","UTF-8"); p.setProperty("connectionCollation","utf8mb4_unicode_ci");
        p.setProperty("connectionTimeZone","UTC"); p.setProperty("forceConnectionTimeZoneToSession","true");
        p.setProperty("preserveInstants","true"); p.setProperty("allowPublicKeyRetrieval","false");
        p.setProperty("allowMultiQueries","false"); p.setProperty("allowLoadLocalInfile","false");
        p.setProperty("connectTimeout","10000"); p.setProperty("socketTimeout","30000");
        String truststore=env.getProperty("acm.db.truststore-path","");
        if(!truststore.isBlank()) {
            Path path=Path.of(truststore).toAbsolutePath();
            if(!java.nio.file.Files.isRegularFile(path)) throw new IllegalArgumentException("Configured truststore file does not exist.");
            p.setProperty("trustCertificateKeyStoreUrl",path.toUri().toString());
            p.setProperty("trustCertificateKeyStoreType","PKCS12");
            p.setProperty("trustCertificateKeyStorePassword",env.getRequiredProperty("acm.db.truststore-password"));
            p.setProperty("fallbackToSystemTrustStore","false");
        } else p.setProperty("fallbackToSystemTrustStore","true");
        return p;
    }
}
