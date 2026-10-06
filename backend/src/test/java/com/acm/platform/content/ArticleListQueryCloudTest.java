package com.acm.platform.content;

import com.acm.platform.auth.AuthService;
import com.acm.platform.db.PlatformRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.jdbc.datasource.AbstractDataSource;
import javax.sql.DataSource;
import java.sql.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Counts actual prepared-statement execution; read-only, no SQL/credentials are logged. */
@SpringBootTest @ActiveProfiles("aiven")
@TestPropertySource(locations="file:secrets/application-private.properties")
@EnabledIfEnvironmentVariable(named="ENABLE_CLOUD_AUTH_TESTS",matches="true")
class ArticleListQueryCloudTest {
 @Autowired DataSource source; @Autowired ObjectMapper mapper; @Autowired AuthService auth;
 @Autowired PlatformRepository original; @Autowired org.springframework.core.env.Environment env;
 @Test void listQueries() {
  assertEquals("development",env.getProperty("acm.db.environment"));
  long adminId=original.jdbc().queryForObject("SELECT MIN(id) FROM users WHERE role='ADMIN' AND username IS NOT NULL",Map.of(),Long.class);
  var admin=auth.account(adminId);var count=new AtomicInteger();
  var service=new ArticleService(new PlatformRepository(new AbstractDataSource(){
   public Connection getConnection()throws SQLException{return wrap(source.getConnection(),count);}
   public Connection getConnection(String username,String password)throws SQLException{return wrap(source.getConnection(username,password),count);}
  },mapper));
  var result=service.list(admin,true,"","",1,10);
  int expected=Integer.getInteger("list.query.expected",2);
  assertEquals(expected,count.get());
  System.out.printf("Article list: rows=%d, actual SQL executions=%d%n",result.items().size(),count.get());
  if(expected==2){
   assertTrue(result.items().stream().noneMatch(x->x.containsKey("sections")||x.containsKey("reviews")||x.containsKey("tagIds")));
   count.set(0);service.list(admin,true,"","",1,1);assertEquals(2,count.get());
  }
 }
 static Object invoke(Method method,Object target,Object[] args)throws Throwable{try{return method.invoke(target,args);}catch(InvocationTargetException e){throw e.getCause();}}
 static Connection wrap(Connection target,AtomicInteger count){return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(proxy,method,args)->{
  Object value=invoke(method,target,args);
  if(value instanceof PreparedStatement statement)return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class[]{PreparedStatement.class},(p,m,a)->{if(Set.of("execute","executeQuery","executeUpdate","executeLargeUpdate").contains(m.getName()))count.incrementAndGet();return invoke(m,statement,a);});
  return value;
 });}
}
