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

@SpringBootTest @AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@ActiveProfiles("aiven") @TestPropertySource(locations="file:secrets/application-private.properties")
@EnabledIfEnvironmentVariable(named="ENABLE_CLOUD_AUTH_TESTS",matches="true") @Transactional
class AccountExperienceCloudTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired AuthService auth;@Autowired ArticleService articles;@Autowired PlatformRepository repo;@Autowired org.springframework.core.env.Environment env;
 String token(MockHttpSession s)throws Exception{return json.readTree(mvc.perform(get("/api/auth/csrf").session(s)).andReturn().getResponse().getContentAsString()).get("token").asText();}
 MvcResult write(String path,Object body,MockHttpSession s,boolean update)throws Exception{return mvc.perform((update?put(path):post(path)).session(s).header("X-CSRF-TOKEN",token(s)).contentType("application/json").content(json.writeValueAsBytes(body))).andReturn();}
 JsonNode data(MvcResult r)throws Exception{return json.readTree(r.getResponse().getContentAsString());}
 void status(int expected,MvcResult r){assertEquals(expected,r.getResponse().getStatus());}
 @Test void ownProfilePasswordAndEveryOldSession()throws Exception {
  assertEquals("development",env.getProperty("acm.db.environment"));String unique="qa_x_"+UUID.randomUUID().toString().replace("-","").substring(0,12),old=UUID.randomUUID()+"-old",next=UUID.randomUUID()+"-new";
  var account=auth.register(new AuthService.Register(unique,"资料验证",old));var a=new MockHttpSession();var b=new MockHttpSession();
  status(200,write("/api/auth/login",new AuthService.Login(unique,old),a,false));status(200,write("/api/auth/login",new AuthService.Login(unique,old),b,false));
  status(400,write("/api/account/profile",Map.of("displayName","越权","role","ADMIN"),a,true));status(400,write("/api/account/profile",Map.of("displayName","越权","userId",account.id()+1),a,true));status(400,write("/api/account/profile",Map.of("displayName","<script>bad</script>"),a,true));
  status(200,write("/api/account/profile",new AuthService.Profile("更新后的中文姓名"),a,true));assertEquals("更新后的中文姓名",data(mvc.perform(get("/api/auth/me").session(b)).andReturn()).at("/user/displayName").asText());
  status(403,mvc.perform(get("/api/admin/users").session(a)).andReturn());status(403,mvc.perform(post("/api/account/password").session(a).contentType("application/json").content("{}")).andReturn());
  status(400,write("/api/account/password",new AuthService.PasswordChange("wrong-old-password",next,next),a,false));status(200,mvc.perform(get("/api/account/articles").session(a)).andReturn());
  status(400,write("/api/account/password",new AuthService.PasswordChange(old,next,next+"x"),a,false));
  status(200,write("/api/account/password",new AuthService.PasswordChange(old,next,next),a,false));assertTrue(a.isInvalid());
  var expired=mvc.perform(get("/api/account/articles").session(b)).andReturn();status(401,expired);assertTrue(b.isInvalid());assertEquals("SESSION_EXPIRED",data(expired).get("code").asText());
  var c=new MockHttpSession();status(401,write("/api/auth/login",new AuthService.Login(unique,old),c,false));status(200,write("/api/auth/login",new AuthService.Login(unique,next),c,false));
  var me=mvc.perform(get("/api/auth/me").session(c)).andReturn();assertFalse(me.getResponse().getContentAsString().contains("credentialVersion"));assertFalse(me.getResponse().getContentAsString().contains("password"));
  assertEquals(1,auth.account(account.id()).credentialVersion());
 }
 @Test void draftIdentityRevisionLanguageSummaryAndQueue()throws Exception {
  assertEquals("development",env.getProperty("acm.db.environment"));String unique="qa_w_"+UUID.randomUUID().toString().replace("-","").substring(0,12);
  var user=auth.register(new AuthService.Register(unique,"写作验证",UUID.randomUUID().toString()));var admin=new Account(user.id(),user.username(),user.displayName(),"ADMIN",user.createdAt());
  long category=repo.jdbc().queryForObject("SELECT MIN(id) FROM categories",Map.of(),Long.class);String key=UUID.randomUUID().toString(),code="print('中文 <script>😀')\n# second line\n";
  var input=new ArticleInput("写作测试 "+unique,"中文摘要",category,List.of(),List.of(new ArticleInput.Section("Python 示例",3,List.of("正文\n换行"),List.of("列表"),code,"python")),0,key);
  var created=articles.create(user,input);long id=((Number)created.get("id")).longValue();assertEquals(id,articles.create(user,input).get("id"));
  assertEquals(1L,repo.jdbc().queryForObject("SELECT COUNT(*) FROM articles WHERE author_id=:id AND draft_key=:key",Map.of("id",user.id(),"key",key),Long.class));
  articles.save(user,id,new ArticleInput(input.title()+"更新",input.summary(),category,List.of(),input.sections(),0));
  assertEquals(409,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->articles.save(user,id,input)).getStatusCode().value());
  assertEquals(input.title()+"更新",articles.detail(user,id).get("title"));
  articles.transition(user,id,"submit",new ArticleService.Action(1,""));articles.transition(admin,id,"reject",new ArticleService.Action(2,"补充代码说明"));
  var summary=articles.list(user,false,unique,"REJECTED",1,10,"submitted","asc").items().getFirst();assertEquals("补充代码说明",summary.get("rejectionReason"));assertFalse(summary.containsKey("sections"));assertFalse(summary.containsKey("reviews"));
  articles.transition(user,id,"submit",new ArticleService.Action(3,""));var second=articles.create(user,new ArticleInput(input.title()+"第二篇",input.summary(),category,List.of(),input.sections(),0,UUID.randomUUID().toString()));long secondId=((Number)second.get("id")).longValue();articles.transition(user,secondId,"submit",new ArticleService.Action(0,""));
  assertEquals(2,articles.list(admin,true,unique,"PENDING",1,10,"submitted","asc").total());assertEquals(id,articles.list(admin,true,unique,"PENDING",1,1,"submitted","asc").items().getFirst().get("id"));assertEquals(secondId,articles.list(admin,true,unique,"PENDING",1,1,"submitted","desc").items().getFirst().get("id"));
  articles.transition(admin,id,"approve",new ArticleService.Action(4,""));assertEquals("python",json.valueToTree(repo.article(id).orElseThrow()).at("/sections/0/codeLanguage").asText());assertEquals(code,json.valueToTree(repo.article(id).orElseThrow()).at("/sections/0/code").asText());
  assertEquals(409,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->articles.save(user,id,new ArticleInput(input.title(),input.summary(),category,List.of(),input.sections(),5))).getStatusCode().value());
  articles.save(admin,id,new ArticleInput("管理员公开更新 "+unique,input.summary(),category,List.of(),input.sections(),5));assertEquals("管理员公开更新 "+unique,repo.article(id).orElseThrow().get("title"));
  auth.updateProfile(user,new AuthService.Profile("新的文章作者名"));assertEquals("新的文章作者名",repo.article(id).orElseThrow().get("author"));
 }
}

