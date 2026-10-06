package com.acm.platform.db;
import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Component @Profile({"mysql-local","aiven"})
public class DatabaseLifecycle implements ApplicationRunner {
 final DataSource source;final Environment env;final DevelopmentSeed seed;final DatabaseVerification verification;final com.acm.platform.auth.AuthService auth;
 public DatabaseLifecycle(DataSource source,Environment env,DevelopmentSeed seed,DatabaseVerification verification,com.acm.platform.auth.AuthService auth){this.source=source;this.env=env;this.seed=seed;this.verification=verification;this.auth=auth;}
 public void run(ApplicationArguments arguments)throws Exception{
  String action=env.getProperty("acm.db.action","serve");
  if(!Set.of("check","inspect","migrate","seed","verify","bootstrap-admin","serve").contains(action))throw new IllegalStateException("Unknown DB_ACTION. No database changes made.");
  int tables;boolean history;
  try(Connection connection=source.getConnection()){
   var meta=connection.getMetaData();
   if(!meta.getDatabaseProductName().equals("MySQL"))throw new IllegalStateException("Only MySQL is supported.");
   String version;try(var s=connection.createStatement();var r=s.executeQuery("SELECT VERSION()")){r.next();version=r.getString(1);}
   checkVersion(version,env.getRequiredProperty("acm.db.expected-major",Integer.class));
   try(var s=connection.prepareStatement("SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema=DATABASE()" );var r=s.executeQuery()){
    tables=0;history=false;while(r.next()){tables++;if(r.getString(1).equals("flyway_schema_history"))history=true;}
   }
   if(action.equals("inspect")) {
    System.out.println("Database environment: "+env.getProperty("acm.db.environment")+"; MySQL: "+version);
    try(var s=connection.createStatement();var r=s.executeQuery("SELECT @@session.wait_timeout,@@global.max_connections")){r.next();System.out.println("Idle timeout seconds: "+r.getInt(1)+"; server connection limit: "+r.getInt(2));}
    for(String sql:List.of("SELECT role,COUNT(*) FROM users GROUP BY role", "SELECT status,COUNT(*) FROM articles GROUP BY status", "SELECT COUNT(*) FROM users WHERE password_hash IS NOT NULL", "SELECT CONSTRAINT_NAME,CHECK_CLAUSE FROM information_schema.check_constraints WHERE constraint_schema=DATABASE() AND (CONSTRAINT_NAME LIKE 'users_%' OR CONSTRAINT_NAME LIKE 'articles_%')")) {
     try(var s=connection.createStatement();var r=s.executeQuery(sql)){while(r.next()){var fields=new ArrayList<String>();for(int i=1;i<=r.getMetaData().getColumnCount();i++)fields.add(r.getString(i));System.out.println(String.join(" | ",fields));}}
    }
    return;
   }
   if(action.equals("check")){
    System.out.println("Database connection verified. MySQL version: "+version+"; existing tables: "+tables+"; Flyway history present: "+history);
    try(var s=connection.createStatement();var r=s.executeQuery("SHOW SESSION STATUS LIKE 'Ssl_cipher'")){boolean tls=r.next()&&!r.getString(2).isBlank();System.out.println("TLS cipher negotiated: "+tls);if(env.acceptsProfiles(org.springframework.core.env.Profiles.of("aiven"))&&!tls)throw new IllegalStateException("Cloud connection is not encrypted.");}
    try(var s=connection.createStatement();var r=s.executeQuery("SHOW GRANTS FOR CURRENT_USER")){
     Set<String> permissions=new TreeSet<>();while(r.next()){String grant=r.getString(1);for(String permission:List.of("SELECT","INSERT","UPDATE","DELETE","CREATE","ALTER","INDEX","REFERENCES"))if(grant.contains("ALL PRIVILEGES")||grant.contains(permission))permissions.add(permission);}
     System.out.println("Detected grant keywords (confirm scope separately): "+permissions);
    }
    return;
   }
  }
  if(tables>0&&!history)throw new IllegalStateException("Existing database contains tables without Flyway history. Migration halted; inspect and plan adoption manually. No baseline or reset was attempted.");
  var flyway=Flyway.configure().dataSource(source).locations("classpath:db/migration").createSchemas(false).cleanDisabled(true).baselineOnMigrate(false).load();
  if(action.equals("migrate")){var result=flyway.migrate();System.out.println("Flyway migration completed; executed migrations: "+result.migrationsExecuted);return;}
  flyway.validate();if(!history||flyway.info().pending().length>0)throw new IllegalStateException("Schema migration required. Run DB_ACTION=migrate explicitly before starting the service.");
  if(action.equals("bootstrap-admin")){auth.bootstrap(env.getRequiredProperty("acm.admin.username"),env.getProperty("acm.admin.display-name","站点管理员"),env.getRequiredProperty("acm.admin.password"));}
  if(action.equals("seed")){seed.importContent();System.out.println("Development content import completed; existing matching IDs were preserved.");}
  if(action.equals("verify")){verification.verify();System.out.println("Database verification completed with rolled-back dedicated test data.");}
 }
 static void checkVersion(String version,int expected){
  if(!version.matches("[0-9]+\\.[0-9]+\\.[0-9]+.*"))throw new IllegalStateException("Unrecognized MySQL version.");
  String[] pieces=version.split("[.-]");int major=Integer.parseInt(pieces[0]),minor=Integer.parseInt(pieces[1]),patch=Integer.parseInt(pieces[2]);
  if(major!=expected||major<8||(major==8&&minor==0&&patch<16))throw new IllegalStateException("MySQL version mismatch or unsupported server; requires MySQL >=8.0.16 and configured expected major. No migration was executed.");
 }
}
