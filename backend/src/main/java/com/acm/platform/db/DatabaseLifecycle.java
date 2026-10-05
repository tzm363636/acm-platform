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
 final DataSource source;final Environment env;final DevelopmentSeed seed;final DatabaseVerification verification;
 public DatabaseLifecycle(DataSource source,Environment env,DevelopmentSeed seed,DatabaseVerification verification){this.source=source;this.env=env;this.seed=seed;this.verification=verification;}
 public void run(ApplicationArguments arguments)throws Exception{
  String action=env.getProperty("acm.db.action","serve");
  if(!Set.of("check","migrate","seed","verify","serve").contains(action))throw new IllegalStateException("Unknown DB_ACTION. No database changes made.");
  int tables;boolean history;
  try(Connection connection=source.getConnection()){
   var meta=connection.getMetaData();
   if(!meta.getDatabaseProductName().equals("MySQL"))throw new IllegalStateException("Only MySQL is supported.");
   String version;try(var s=connection.createStatement();var r=s.executeQuery("SELECT VERSION()")){r.next();version=r.getString(1);}
   checkVersion(version,env.getRequiredProperty("acm.db.expected-major",Integer.class));
   try(var s=connection.prepareStatement("SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema=DATABASE()" );var r=s.executeQuery()){
    tables=0;history=false;while(r.next()){tables++;if(r.getString(1).equals("flyway_schema_history"))history=true;}
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
  if(action.equals("seed")){seed.importContent();System.out.println("Development content import completed; existing matching IDs were preserved.");}
  if(action.equals("verify")){verification.verify();System.out.println("Database verification completed with rolled-back dedicated test data.");}
 }
 static void checkVersion(String version,int expected){
  if(!version.matches("[0-9]+\\.[0-9]+\\.[0-9]+.*"))throw new IllegalStateException("Unrecognized MySQL version.");
  String[] pieces=version.split("[.-]");int major=Integer.parseInt(pieces[0]),minor=Integer.parseInt(pieces[1]),patch=Integer.parseInt(pieces[2]);
  if(major!=expected||major<8||(major==8&&minor==0&&patch<16))throw new IllegalStateException("MySQL version mismatch or unsupported server; requires MySQL >=8.0.16 and configured expected major. No migration was executed.");
 }
}
