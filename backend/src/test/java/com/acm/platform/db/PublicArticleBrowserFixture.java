package com.acm.platform.db;

import com.acm.platform.controller.DataController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.springframework.web.server.ResponseStatusException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Manual browser fixture, bound only to loopback. H2 memory only, never reads private config.
 * Optional argument: local mode file containing ok, fail or slow (test-only fault injection).
 * Run with test classpath; not included in the production jar. */
public class PublicArticleBrowserFixture {
 public static void main(String[] args)throws Exception {
  var fixture=new PublicArticleQueryTest();fixture.setup();
  var controller=new DataController(fixture.repo,null);var json=new ObjectMapper();
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",8081),0);
  var requests=new AtomicInteger();Path mode=args.length>0?Path.of(args[0]):null;
  server.createContext("/api/",exchange->{
   int status=200;Object body;
   var path=exchange.getRequestURI().getPath();var query=new HashMap<String,String>();
   String raw=exchange.getRequestURI().getRawQuery();
   if(raw!=null)for(String pair:raw.split("&")){var kv=pair.split("=",2);query.put(URLDecoder.decode(kv[0],StandardCharsets.UTF_8),kv.length>1?URLDecoder.decode(kv[1],StandardCharsets.UTF_8):"");}
   try {
    if(!exchange.getRequestMethod().equals("GET"))throw new ResponseStatusException(org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED);
    String scenario=mode!=null&&Files.exists(mode)?Files.readString(mode).trim():"ok";
    if(path.startsWith("/api/articles")){
     int count=requests.incrementAndGet();System.out.println("Public article HTTP request #"+count);
     if(scenario.equals("slow"))Thread.sleep(4000);
     if(scenario.equals("fail"))throw new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"隔离测试：文章服务暂不可用。");
    }
    if(path.equals("/api/articles"))body=controller.articles(query.getOrDefault("q",""),query.getOrDefault("category",""),query.getOrDefault("tag",""),query.getOrDefault("layout",""),Integer.parseInt(query.getOrDefault("page","1")),Integer.parseInt(query.getOrDefault("size","6")),query.getOrDefault("sort","id"),Boolean.parseBoolean(query.getOrDefault("includeOptions","false")));
    else if(path.matches("/api/articles/[0-9]+"))body=controller.article(Long.parseLong(path.substring(path.lastIndexOf('/')+1)));
    else if(path.equals("/api/article-options"))body=controller.articleOptions();
    else if(path.equals("/api/auth/me")){var anonymous=new HashMap<String,Object>();anonymous.put("user",null);body=anonymous;}
    else {status=404;body=Map.of("message","此隔离服务只提供公开文章验证接口。");}
   }catch(ResponseStatusException e){status=e.getStatusCode().value();body=Map.of("message",Objects.toString(e.getReason(),"请求失败。"));}
   catch(Exception e){status=500;body=Map.of("message","隔离验证接口失败。");}
   byte[] bytes=json.writeValueAsBytes(body);exchange.getResponseHeaders().set("Content-Type","application/json;charset=UTF-8");exchange.getResponseHeaders().set("Cache-Control","no-store");
   try{exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);}finally{exchange.close();}
  });
  server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());server.start();
  System.out.println("Isolated public-article fixture on 127.0.0.1:8081; 125 published + 4 private; no Aiven access.");
 }
}
