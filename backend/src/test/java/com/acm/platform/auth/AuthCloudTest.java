package com.acm.platform.auth;

import com.acm.platform.content.*;
import com.acm.platform.db.PlatformRepository;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Explicit development-cloud integration test. All dedicated accounts/articles are rolled back. */
@SpringBootTest @AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE) @ActiveProfiles("aiven")
@TestPropertySource(locations="file:secrets/application-private.properties")
@EnabledIfEnvironmentVariable(named="ENABLE_CLOUD_AUTH_TESTS",matches="true")
@Transactional
class AuthCloudTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired PlatformRepository repo;@Autowired AuthService auth;@Autowired ArticleService articles;
 @Autowired org.springframework.core.env.Environment env;
 MockHttpSession session(){return new MockHttpSession();}
 String token(MockHttpSession s)throws Exception{return json.readTree(mvc.perform(get("/api/auth/csrf").session(s)).andReturn().getResponse().getContentAsString()).get("token").asText();}
 MvcResult write(String path,Object body,MockHttpSession s,boolean put)throws Exception{return mvc.perform((put?put(path):post(path)).session(s).header("X-CSRF-TOKEN",token(s)).contentType("application/json").content(json.writeValueAsBytes(body))).andReturn();}
 JsonNode data(MvcResult r)throws Exception{return json.readTree(r.getResponse().getContentAsString());}
 void status(int expected,MvcResult r){assertEquals(expected,r.getResponse().getStatus());}
 @Test void completeWorkflowAndSecurity()throws Exception {
  assertEquals("development",env.getProperty("acm.db.environment"));
  String unique="qa_"+UUID.randomUUID().toString().replace("-","").substring(0,12),password=UUID.randomUUID()+"-test";
  var userSession=session();var secondSession=session();var adminSession=session();
  status(401,mvc.perform(get("/api/admin/users")).andReturn());status(401,mvc.perform(post("/api/account/articles").contentType("application/json").content("{}")).andReturn());
  status(200,mvc.perform(get("/api/oj/submissions").param("mode","demo")).andReturn());
  status(401,mvc.perform(post("/api/oj/submissions").contentType("application/json").content("{}")).andReturn());
  status(403,mvc.perform(post("/api/auth/register").contentType("application/json").content("{}")).andReturn());
  status(400,write("/api/auth/register",Map.of("username",unique,"displayName","测试用户","password",password,"role","ADMIN"),userSession,false));
  var registered=write("/api/auth/register",new AuthService.Register(unique,"测试用户",password),userSession,false);status(200,registered);assertEquals("USER",data(registered).at("/user/role").asText());
  String oldSessionId=userSession.getId();status(200,write("/api/auth/login",new AuthService.Login(unique,password),userSession,false));assertNotEquals(oldSessionId,userSession.getId());
  var me=mvc.perform(get("/api/auth/me").session(userSession)).andReturn();status(200,me);assertFalse(me.getResponse().getContentAsString().contains("password"));long userId=data(me).at("/user/id").asLong();
  status(403,mvc.perform(get("/api/admin/users").session(userSession)).andReturn());
  status(403,write("/api/admin/tags",Map.of("name","越权标签"),userSession,false));
  var second=auth.register(new AuthService.Register(unique+"b","第二用户",password));status(200,write("/api/auth/login",new AuthService.Login(unique+"b",password),secondSession,false));
  var administrator=auth.register(new AuthService.Register(unique+"a","测试管理员",password));repo.jdbc().update("UPDATE users SET role='ADMIN' WHERE id=:id",Map.of("id",administrator.id()));status(200,write("/api/auth/login",new AuthService.Login(unique+"a",password),adminSession,false));administrator=auth.account(administrator.id());
  long category=repo.jdbc().queryForObject("SELECT MIN(id) FROM categories",Map.of(),Long.class);long tag=repo.jdbc().queryForObject("SELECT MIN(id) FROM tags",Map.of(),Long.class);
  String taxonomyName="验证标签 "+unique;
  status(200,write("/api/admin/tags",new ArticleService.Name(taxonomyName),adminSession,false));
  long createdTag=repo.jdbc().queryForObject("SELECT id FROM tags WHERE name=:name",Map.of("name",taxonomyName),Long.class);
  status(409,write("/api/admin/tags",new ArticleService.Name(taxonomyName),adminSession,false));
  status(200,write("/api/admin/tags/"+createdTag,new ArticleService.Name(taxonomyName+" 修改"),adminSession,true));
  assertEquals(taxonomyName+" 修改",repo.jdbc().queryForObject("SELECT name FROM tags WHERE id=:id",Map.of("id",createdTag),String.class));
  var input=new ArticleInput("中文审核测试 "+unique,"中文摘要 😀",category,List.of(tag),List.of(new ArticleInput.Section("二级标题",2,List.of("正文与换行\n第二行"),List.of("列表项"),"int main() {\n  // 中文代码 😀\n  return 0;\n}")),0);
  var created=write("/api/account/articles",input,userSession,false);status(200,created);long id=data(created).get("id").asLong();assertEquals("DRAFT",data(created).get("status").asText());
  status(404,mvc.perform(get("/api/articles/"+id)).andReturn());status(403,write("/api/account/articles/"+id,input,secondSession,true));
  status(400,write("/api/account/articles/"+id,Map.of("title","恶意","authorId",second.id(),"status","PUBLISHED"),userSession,true));
  var pending=write("/api/account/articles/"+id+"/submit",new ArticleService.Action(0,""),userSession,false);status(200,pending);int revision=data(pending).get("revision").asInt();
  var lockedInput=new ArticleInput(input.title(),input.summary(),category,List.of(tag),input.sections(),revision);status(409,write("/api/account/articles/"+id,lockedInput,userSession,true));
  assertEquals(0,data(mvc.perform(get("/api/articles").param("q",unique)).andReturn()).get("total").asInt());
  status(400,write("/api/admin/articles/"+id+"/reject",new ArticleService.Action(revision,""),adminSession,false));
  var rejected=write("/api/admin/articles/"+id+"/reject",new ArticleService.Action(revision,"补充说明后再投稿"),adminSession,false);status(200,rejected);revision=data(rejected).get("revision").asInt();assertEquals("补充说明后再投稿",data(rejected).at("/reviews/0/reason").asText());
  var saved=write("/api/account/articles/"+id,new ArticleInput(input.title(),input.summary(),category,List.of(tag),input.sections(),revision),userSession,true);status(200,saved);revision=data(saved).get("revision").asInt();
  pending=write("/api/account/articles/"+id+"/submit",new ArticleService.Action(revision,""),userSession,false);status(200,pending);revision=data(pending).get("revision").asInt();
  status(200,write("/api/account/articles/"+id+"/withdraw",new ArticleService.Action(revision,""),userSession,false));
  pending=write("/api/account/articles/"+id+"/submit",new ArticleService.Action(revision+1,""),userSession,false);status(200,pending);revision=data(pending).get("revision").asInt();
  status(200,write("/api/admin/articles/"+id+"/approve",new ArticleService.Action(revision,""),adminSession,false));status(409,write("/api/admin/articles/"+id+"/approve",new ArticleService.Action(revision,""),adminSession,false));
  var publicArticle=mvc.perform(get("/api/articles/"+id)).andReturn();status(200,publicArticle);assertEquals(input.sections().getFirst().code(),data(publicArticle).at("/sections/0/code").asText());
  assertTrue(data(publicArticle).get("authorProfile").isNull());
  assertEquals(1,data(mvc.perform(get("/api/articles").param("q",unique)).andReturn()).get("total").asInt());
  var managed=articles.detail(administrator,id);int currentRevision=((Number)managed.get("revision")).intValue();
  articles.flags(administrator,id,new ArticleService.Flags(currentRevision,true,true));
  assertEquals(1,repo.articles(unique,"","","featured",1,10).total());
  assertEquals(0,repo.articles(unique,"","","wide",1,10).total());
  assertEquals(0,repo.articles(unique,"","","regular",1,10).total());
  assertEquals(2,repo.jdbc().queryForObject("SELECT COUNT(*) FROM article_reviews ar JOIN articles a ON a.id=ar.article_id WHERE a.public_id=:id",Map.of("id",id),Long.class));
  var direct=articles.create(administrator,input);long directId=((Number)direct.get("id")).longValue();articles.transition(administrator,directId,"publish",new ArticleService.Action(0,""));
  status(200,mvc.perform(get("/api/articles/"+directId)).andReturn());articles.transition(administrator,directId,"archive",new ArticleService.Action(1,"测试下架"));status(404,mvc.perform(get("/api/articles/"+directId)).andReturn());
  var users=mvc.perform(get("/api/admin/users").session(adminSession)).andReturn();status(200,users);assertFalse(users.getResponse().getContentAsString().contains("password_hash"));
  status(200,write("/api/auth/logout",Map.of(),userSession,false));assertTrue(userSession.isInvalid());assertTrue(data(mvc.perform(get("/api/auth/me")).andReturn()).get("user").isNull());
  assertEquals(0L,repo.formalPractice(userId).get("passed"));
 }
}
