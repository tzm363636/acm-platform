package com.acm.platform.db;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @Profile({"mysql-local","aiven"})
public class DevelopmentSeed {
 final PlatformRepository repo;final Environment env;
 public DevelopmentSeed(PlatformRepository repo,Environment env){this.repo=repo;this.env=env;}
 void requireDevelopment(){if(!"development".equals(env.getProperty("acm.db.environment")))throw new IllegalStateException("Development seed/verification is disabled outside an explicitly declared development database.");}
 long id(String table,String field,Object value){return repo.jdbc().queryForObject("SELECT id FROM "+table+" WHERE "+field+"=:v",Map.of("v",value),Long.class);}
 long named(String table,String name){var args=Map.of("name",name);repo.jdbc().update("INSERT INTO "+table+"(name) VALUES(:name) ON DUPLICATE KEY UPDATE name=name",args);return id(table,"name",name);}
 @SuppressWarnings("unchecked") @Transactional
 public void importContent() throws Exception {
  requireDevelopment();Map<String,List<Map<String,Object>>> source;
  try(var input=new ClassPathResource("db/seed/site-content.json").getInputStream()){source=repo.mapper().readValue(input,new TypeReference<>(){});}
  repo.jdbc().update("INSERT INTO users(public_id,display_name,role) VALUES('site-author','田振民','AUTHOR') ON DUPLICATE KEY UPDATE public_id=public_id",Map.of());
  long author=id("users","public_id","site-author");
  for(var a:source.get("articles")){
   if(repo.count("SELECT COUNT(*) FROM articles WHERE public_id=:id",Map.of("id",a.get("id")))>0){
    if(!repo.article(((Number)a.get("id")).longValue()).map(x->Objects.equals(x.get("title"),a.get("title"))).orElse(false))throw new IllegalStateException("Existing article ID conflicts with seed; no overwrite performed.");continue;
   }
   var args=PlatformRepository.map("public",a.get("id"),"author",author,"category",named("categories",(String)a.get("category")),"title",a.get("title"),"summary",a.get("summary"),"body",repo.json(a.get("sections")),"preview",repo.json(a.get("preview")),"featured",a.getOrDefault("featured",false),"wide",a.getOrDefault("wide",false));
   repo.jdbc().update("INSERT INTO articles(public_id,author_id,category_id,title,summary,body,preview,featured,wide,status,data_kind,published_at) VALUES(:public,:author,:category,:title,:summary,:body,:preview,:featured,:wide,'PUBLISHED','REAL',UTC_TIMESTAMP(6))",args);
   long internal=id("articles","public_id",a.get("id"));int position=0;
   for(String tag:(List<String>)a.get("tags"))repo.jdbc().update("INSERT INTO article_tags(article_id,tag_id,position) VALUES(:owner,:tag,:position)",PlatformRepository.map("owner",internal,"tag",named("tags",tag),"position",position++));
  }
  for(var p:source.get("problems")){
   if(repo.count("SELECT COUNT(*) FROM problems WHERE public_id=:id",Map.of("id",p.get("id")))>0){if(!repo.problem((String)p.get("id")).map(x->Objects.equals(x.get("title"),p.get("title"))).orElse(false))throw new IllegalStateException("Existing problem ID conflicts with seed; no overwrite performed.");continue;}
   var args=PlatformRepository.map("public",p.get("id"),"title",p.get("title"),"difficulty",p.get("difficulty"),"description",p.get("description"),"input",p.get("input"),"output",p.get("output"),"constraints",repo.json(p.get("constraints")),"template",p.get("template"),"time",p.get("timeLimit"),"memory",p.get("memoryLimit"),"source",p.get("source"));
   repo.jdbc().update("INSERT INTO problems(public_id,title,difficulty,description,input_format,output_format,constraints_json,template_cpp17,time_limit_ms,memory_limit_mb,source,status,data_kind) VALUES(:public,:title,:difficulty,:description,:input,:output,:constraints,:template,:time,:memory,:source,'PUBLISHED','DEMO')",args);
   long internal=id("problems","public_id",p.get("id"));int position=0;
   for(String tag:(List<String>)p.get("tags"))repo.jdbc().update("INSERT INTO problem_tags(problem_id,tag_id,position) VALUES(:owner,:tag,:position)",PlatformRepository.map("owner",internal,"tag",named("tags",tag),"position",position++));
   position=0;for(var sample:(List<Map<String,Object>>)p.get("samples")) repo.jdbc().update("INSERT INTO problem_samples(problem_id,position,input_text,output_text,explanation) VALUES(:owner,:position,:input,:output,:explanation)",PlatformRepository.map("owner",internal,"position",position++,"input",sample.get("input"),"output",sample.get("output"),"explanation",sample.get("explanation")));
   position=0;for(var a:(List<Number>)p.get("articleIds"))repo.jdbc().update("INSERT INTO problem_articles(problem_id,article_id,position) VALUES(:owner,:article,:position)",PlatformRepository.map("owner",internal,"article",id("articles","public_id",a),"position",position++));
  }
 }
}
