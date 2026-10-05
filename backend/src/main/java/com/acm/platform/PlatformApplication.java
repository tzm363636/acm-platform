package com.acm.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import java.util.Arrays;

@SpringBootApplication
public class PlatformApplication {
    public static void main(String[] args) {
        SpringApplication application=new SpringApplication(PlatformApplication.class);
        String action=System.getenv().getOrDefault("DB_ACTION","serve");
        for(String arg:args)if(arg.startsWith("--acm.db.action="))action=arg.substring("--acm.db.action=".length());
        if(!action.equals("serve"))application.setWebApplicationType(WebApplicationType.NONE);
        try {
            ConfigurableApplicationContext context=application.run(args);
            if(!action.equals("serve"))context.close();
        } catch(Exception exception) {
            System.err.println("Backend startup/database operation failed. Check private configuration, TLS, permissions and schema. No credentials are printed by the application.");
            Throwable reason=exception;
            for(int depth=0;depth<12&&reason!=null;depth++,reason=reason.getCause()){
                System.err.println("Diagnostic type: "+reason.getClass().getSimpleName());
                Arrays.stream(reason.getStackTrace()).filter(frame->frame.getClassName().startsWith("com.acm.platform")).limit(4).forEach(frame->System.err.println(frame.getClassName()+"."+frame.getMethodName()+":"+frame.getLineNumber()));
                if(reason instanceof java.sql.SQLException sql)System.err.println("SQLState="+sql.getSQLState()+"; vendorCode="+sql.getErrorCode());
            }
            Thread.getAllStackTraces().forEach((thread,stack)->{
                if(thread.getName().contains("acm-mysql:connection-adder")){
                    System.err.println("Connection worker state: "+thread.getState());
                    Arrays.stream(stack).limit(10).forEach(frame->System.err.println(frame.getClassName()+"."+frame.getMethodName()));
                }
            });
            System.exit(1);
        }
    }
}
