package com.acm.platform.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;
import java.sql.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Isolated JDBC fixture: no cloud credentials and no production writes. */
class PublicArticleQueryTest {
 PlatformRepository repo; JdbcTemplate fixture; AtomicInteger queries;
 @BeforeEach void setup() {
  var source=new JdbcDataSource();
  source.setURL("jdbc:h2:mem:articles_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");
  fixture=new JdbcTemplate(source);
  fixture.execute("CREATE TABLE users(id BIGINT PRIMARY KEY,public_id VARCHAR(64),display_name VARCHAR(100))");
  fixture.execute("CREATE TABLE categories(id BIGINT PRIMARY KEY,name VARCHAR(64))");
  fixture.execute("CREATE TABLE tags(id BIGINT PRIMARY KEY,name VARCHAR(64))");
  fixture.execute("CREATE TABLE articles(id BIGINT PRIMARY KEY,public_id BIGINT UNIQUE,author_id BIGINT REFERENCES users(id),category_id BIGINT REFERENCES categories(id),title VARCHAR(255),summary VARCHAR(2000),body VARCHAR(10000),preview VARCHAR(2000),featured BOOLEAN,wide BOOLEAN,status VARCHAR(16),published_at TIMESTAMP,updated_at TIMESTAMP)");
  fixture.execute("CREATE TABLE article_tags(article_id BIGINT REFERENCES articles(id),tag_id BIGINT REFERENCES tags(id),position INT,PRIMARY KEY(article_id,tag_id))");
  fixture.update("INSERT INTO users VALUES(1,'site-author','测试作者')");
  fixture.update("INSERT INTO categories VALUES(1,'算法模板'),(2,'题解'),(3,'仅私人分类')");
  fixture.update("INSERT INTO tags VALUES(1,'图论'),(2,'C++'),(3,'私人标签')");
  for(int i=1;i<=129;i++) {
   String status=i<=125?"PUBLISHED":List.of("DRAFT","PENDING","REJECTED","ARCHIVED").get(i-126);
   fixture.update("INSERT INTO articles VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",i,1000+i,1,i<=125?(i%2+1):3,"中文文章 "+i,"摘要 "+i,"[{\"heading\":\"中文\",\"paragraphs\":[\"正文\"],\"code\":\"int main() {\\n  return 0;\\n}\\n\"}]","[\"int main() {\"]",i%7==0,i%11==0,status,i<=125?Timestamp.valueOf("2026-10-01 00:00:00"):null,Timestamp.valueOf("2026-10-02 00:00:00"));
   fixture.update("INSERT INTO article_tags VALUES(?,?,0)",i,i<=125?1:3);
   if(i<=125)fixture.update("INSERT INTO article_tags VALUES(?,2,1)",i);
  }
  queries=new AtomicInteger();
  repo=new PlatformRepository(new AbstractDataSource(){
   public Connection getConnection()throws SQLException{return wrap(source.getConnection());}
   public Connection getConnection(String u,String p)throws SQLException{return wrap(source.getConnection(u,p));}
  },new ObjectMapper());
 }
 Connection wrap(Connection target) {
  return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
   Object value=invoke(m,target,a);
   if(value instanceof PreparedStatement statement)return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class[]{PreparedStatement.class},(p2,m2,a2)->{
    if(Set.of("execute","executeQuery","executeUpdate").contains(m2.getName()))queries.incrementAndGet();return invoke(m2,statement,a2);
   });return value;
  });
 }
 static Object invoke(Method m,Object target,Object[] a)throws Throwable{try{return m.invoke(target,a);}catch(InvocationTargetException e){throw e.getCause();}}
 @Test void boundedSummaryQueries() {
  var result=repo.articles("","","","",1,8);
  assertEquals(125,result.total());assertEquals(8,result.items().size());assertEquals(3,queries.get());
  assertTrue(result.items().stream().noneMatch(a->a.containsKey("sections")||a.containsKey("body")||a.containsKey("reviews")));
  assertEquals(List.of("图论","C++"),result.items().getFirst().get("tags"));
  System.out.printf("Public article summary: rows=%d, SQL executions=%d%n",result.items().size(),queries.get());
  queries.set(0);repo.articles("","","","",1,100);assertEquals(3,queries.get());
  queries.set(0);repo.articleFeed("","","","",1,8,"published");assertEquals(5,queries.get());
  System.out.println("Public page with options and totals: SQL executions=5");
 }
 @Test void all125AcrossStableSortedPages() {
  for(String sort:List.of("published","updated","recommended")) {
   var seen=new LinkedHashSet<Long>();
   for(int p=1;p<=11;p++) {
    var page=repo.articles("","","","",p,12,sort);
    assertEquals(125,page.total());assertEquals(11,page.pages());assertEquals((p-1)*12+1,page.start());assertEquals(Math.min(125,p*12),page.end());
    for(var a:page.items())assertTrue(seen.add(((Number)a.get("id")).longValue()),"duplicate across pages");
   }
   assertEquals(125,seen.size());
   if(!sort.equals("recommended"))assertEquals(1125L,seen.iterator().next());
  }
 }
 @Test void combinedFiltersEscapeAndPageBoundaries() {
  var page=repo.articles("中文文章 1","算法模板","C++","",1,6,"published");
  assertTrue(page.total()>0);assertTrue(page.items().stream().allMatch(a->a.get("title").toString().contains("文章 1")&&a.get("category").equals("算法模板")));
  assertEquals(0,repo.articles("%_!","","","",1,8).total());
  var missing=repo.articles("不存在","","","",5,8);assertEquals(0,missing.start());assertEquals(0,missing.end());assertEquals(1,missing.page());
  var last=repo.articles("","","","",999,12);assertEquals(11,last.page());assertEquals(5,last.items().size());
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->repo.articles("","","","",1,8,"published; DELETE FROM articles"));
 }
 @Test void publicVisibilityMetadataAndRefresh() {
  for(long id=1126;id<=1129;id++)assertTrue(repo.article(id).isEmpty());
  var opts=repo.articleOptions();assertEquals(125L,opts.get("publishedTotal"));assertEquals(2,opts.get("categoryCount"));
  assertFalse(opts.get("tags").toString().contains("私人"));assertFalse(opts.get("categories").toString().contains("私人"));
  fixture.update("UPDATE articles SET status='ARCHIVED' WHERE id=1");
  assertEquals(124L,repo.articleOptions().get("publishedTotal"));assertTrue(repo.article(1001).isEmpty());
  fixture.update("UPDATE articles SET status='PUBLISHED' WHERE id=126");
  assertEquals(125L,repo.articleOptions().get("publishedTotal"));assertEquals(3,repo.articleOptions().get("categoryCount"));
 }
 @Test void detailKeepsBodyNewlinesAndNullableTimes() {
  var detail=repo.article(1001).orElseThrow();
  assertTrue(detail.get("publishedAt").toString().endsWith("Z"));assertTrue(detail.get("updatedAt").toString().endsWith("Z"));
  var sections=(List<?>)detail.get("sections");var section=(Map<?,?>)sections.getFirst();
  assertEquals("中文",section.get("heading"));assertEquals("int main() {\n  return 0;\n}\n",section.get("code"));
  fixture.update("UPDATE articles SET published_at=NULL,updated_at=NULL WHERE id=1");
  detail=repo.article(1001).orElseThrow();assertNull(detail.get("publishedAt"));assertNull(detail.get("updatedAt"));
 }
}
