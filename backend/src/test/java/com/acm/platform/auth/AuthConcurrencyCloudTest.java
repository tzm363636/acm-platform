package com.acm.platform.auth;
import com.acm.platform.content.*;
import com.acm.platform.db.PlatformRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.core.env.Environment;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Commits only UUID-scoped dedicated fixtures, then removes those exact fixtures in finally. */
@SpringBootTest @org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE) @ActiveProfiles("aiven") @TestPropertySource(locations="file:secrets/application-private.properties")
@EnabledIfEnvironmentVariable(named="ENABLE_CLOUD_AUTH_TESTS",matches="true")
class AuthConcurrencyCloudTest {
 @Autowired AuthService auth;@Autowired ArticleService articles;@Autowired PlatformRepository repo;@Autowired Environment env;
 @Test void concurrentFirstSaveCreatesOneDraft()throws Exception {
  assertEquals("development",env.getProperty("acm.db.environment"));String name="qa_d_"+UUID.randomUUID().toString().replace("-","").substring(0,12),key=UUID.randomUUID().toString();
  var user=auth.register(new AuthService.Register(name,"草稿去重验证",UUID.randomUUID().toString()));long publicId=0;var executor=Executors.newFixedThreadPool(2);
  try{
   long category=repo.jdbc().queryForObject("SELECT MIN(id) FROM categories",Map.of(),Long.class);
   var input=new ArticleInput("草稿去重 "+name,"专用验证数据",category,List.of(),List.of(new ArticleInput.Section("章节",2,List.of("内容"),List.of(),null)),0,key);
   var gate=new CountDownLatch(1);Callable<Long> task=()->{gate.await();return ((Number)articles.create(user,input).get("id")).longValue();};var first=executor.submit(task);var second=executor.submit(task);gate.countDown();publicId=first.get(45,TimeUnit.SECONDS);assertEquals(publicId,second.get(45,TimeUnit.SECONDS));
   assertEquals(1L,repo.jdbc().queryForObject("SELECT COUNT(*) FROM articles WHERE author_id=:id AND draft_key=:key",Map.of("id",user.id(),"key",key),Long.class));
  }finally{executor.shutdownNow();repo.jdbc().update("DELETE FROM articles WHERE author_id=:owner AND draft_key=:key",Map.of("owner",user.id(),"key",key));repo.jdbc().update("DELETE FROM users WHERE id=:id AND username=:name",Map.of("id",user.id(),"name",name));}
 }
 @Test void twoReviewersOnlyOneTransitionAndOneAudit()throws Exception {
  assertEquals("development",env.getProperty("acm.db.environment"));
  String name="qa_c_"+UUID.randomUUID().toString().replace("-","").substring(0,12);long publicId=0;
  Account user=auth.register(new AuthService.Register(name,"并发验证专用",UUID.randomUUID().toString()));
  // Trusted test principal, never supplied by a browser or created through public registration.
  Account admin=new Account(user.id(),user.username(),user.displayName(),"ADMIN",user.createdAt());
  try {
   long category=repo.jdbc().queryForObject("SELECT MIN(id) FROM categories",Map.of(),Long.class);
   var input=new ArticleInput("并发审核专用 "+name,"验证用数据",category,List.of(),List.of(new ArticleInput.Section("章节",2,List.of("正文"),List.of(),null)),0);
   publicId=((Number)articles.create(user,input).get("id")).longValue();articles.transition(user,publicId,"submit",new ArticleService.Action(0,""));
   final long id=publicId;var gate=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
   try {Callable<Integer> task=()->{gate.await();try{articles.transition(admin,id,"approve",new ArticleService.Action(1,""));return 200;}catch(org.springframework.web.server.ResponseStatusException e){return e.getStatusCode().value();}};
    var one=executor.submit(task);var two=executor.submit(task);gate.countDown();var responses=new ArrayList<>(List.of(one.get(45,TimeUnit.SECONDS),two.get(45,TimeUnit.SECONDS)));Collections.sort(responses);assertEquals(List.of(200,409),responses);
    assertEquals(1L,repo.jdbc().queryForObject("SELECT COUNT(*) FROM article_reviews r JOIN articles a ON a.id=r.article_id WHERE a.public_id=:id",Map.of("id",publicId),Long.class));
    assertEquals("PUBLISHED",articles.detail(user,publicId).get("status"));
   } finally {executor.shutdownNow();}
  } finally {
   if(publicId!=0){var args=Map.of("id",publicId,"owner",user.id());repo.jdbc().update("DELETE r FROM article_reviews r JOIN articles a ON a.id=r.article_id WHERE a.public_id=:id AND a.author_id=:owner",args);repo.jdbc().update("DELETE FROM articles WHERE public_id=:id AND author_id=:owner",args);}
   repo.jdbc().update("DELETE FROM users WHERE id=:id AND username=:name",Map.of("id",user.id(),"name",name));
  }
 }
}
