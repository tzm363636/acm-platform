package com.acm.platform.db;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service @Profile({"mysql-local","aiven"})
public class DemoService {
 final PlatformRepository repo;final Environment env;
 static final Set<String> SCENARIOS=Set.of("AC","WA","CE","RE","TLE","MLE","SystemError","NetworkError");
 public DemoService(PlatformRepository repo,Environment env){this.repo=repo;this.env=env;}
 public record Request(String code,String scenario,String language,String input){}
 public static String session(String token){
  if(token==null||token.isBlank())return "";
  if(!token.matches("[a-zA-Z0-9-]{32,128}"))throw PlatformRepository.bad("无效演示会话。");
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException();}
 }
 void enabled(){if(!env.getProperty("acm.db.demo-enabled",Boolean.class,false)||!"development".equals(env.getProperty("acm.db.environment"))) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"演示写入未开放；需要明确的开发数据库配置。正式登录和判题尚未接入。");}
 void validate(Request r){if(r.code()==null||r.code().isBlank())throw PlatformRepository.bad("代码为空。");if(r.code().length()>262144)throw PlatformRepository.bad("代码过长，最多 256K 字符。");if(!"cpp17".equals(r.language()))throw PlatformRepository.bad("仅开放 C++17 演示语言。");if(!SCENARIOS.contains(r.scenario()))throw PlatformRepository.bad("无效固定演示场景。");if(r.input()!=null&&r.input().length()>262144)throw PlatformRepository.bad("输入过长。");}
 public static String information(String scene){return switch(scene){
  case "CE"->"固定编译错误演示（未编译用户代码）：\nmain.cpp:12:5: error: expected ';' before 'return'\n   12 | return 0;\n      | ^";
  case "RE"->"固定运行错误演示（未执行用户代码）：\nRuntime error: simulated abnormal termination.";
  case "SystemError"->"固定服务异常演示；未执行用户代码。可以重试。";
  default->"固定 "+scene+" 演示场景，未执行、编译或评测用户代码；结果与代码无关。";};}
 @SuppressWarnings("unchecked")
 public Map<String,Object> run(String id,Request request){
  enabled();validate(request);var problem=repo.problem(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"题目不存在。"));
  if(request.scenario().equals("NetworkError"))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"固定网络失败场景；代码与输入已保留。");
  var samples=(List<Map<String,Object>>)problem.get("samples");
  var sample=samples.stream().filter(s->normalize((String)s.get("input")).equals(normalize(Optional.ofNullable(request.input()).orElse("")))).findFirst();
  String output=request.scenario().equals("AC")?(String)sample.orElse(samples.getFirst()).get("output"):request.scenario().equals("WA")?"固定演示错误输出":"";
  return PlatformRepository.map("verdict",request.scenario(),"output",output,"expected",sample.map(s->s.get("output")).orElse(null),"matchedSample",sample.isPresent()&&request.scenario().equals("AC"),"information",information(request.scenario()),"timeMs",time(problem,request.scenario()),"memoryMB",memory(problem,request.scenario()));
 }
 static String normalize(String s){return s.strip().replaceAll("\\s+"," ");}
 static Object time(Map<String,Object> p,String s){
  if(s.equals("TLE"))return ((Number)p.get("timeLimit")).intValue()+1;
  if(Set.of("AC","WA","RE","MLE").contains(s))return 12;
  return null;
 }
 static Object memory(Map<String,Object> p,String s){
  if(s.equals("MLE"))return ((Number)p.get("memoryLimit")).doubleValue()+1;
  if(Set.of("AC","WA","RE","TLE").contains(s))return 2.1;
  return null;
 }
 @Transactional
 public Map<String,Object> submit(String id,Request request,String session){
  enabled();validate(request);if(session.isBlank())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"需要演示会话身份。");
  if(request.scenario().equals("NetworkError"))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"模拟请求失败；未创建提交记录。");
  repo.problem(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"题目不存在。"));
  String user="demo-"+session.substring(0,32);var args=PlatformRepository.map("user",user,"session",session);
  repo.jdbc().update("INSERT INTO users(public_id,display_name,is_demo) VALUES(:user,'本机演示用户',TRUE) ON DUPLICATE KEY UPDATE public_id=public_id",args);
  String submission="DEMO-L"+UUID.randomUUID();args.putAll(PlatformRepository.map("publicId",submission,"problem",id,"code",request.code(),"scene",request.scenario()));
  repo.jdbc().update("INSERT INTO submissions(public_id,user_id,problem_id,language,code,data_kind,origin,demo_session_hash,demo_scenario) SELECT :publicId,u.id,p.id,'cpp17',:code,'DEMO','local',:session,:scene FROM users u CROSS JOIN problems p WHERE u.public_id=:user AND p.public_id=:problem AND p.status='PUBLISHED'",args);
  repo.jdbc().update("INSERT INTO submission_details(submission_id,information) SELECT id,'服务器固定模拟流程，未执行用户代码。' FROM submissions WHERE public_id=:publicId",args);
  return repo.submission(submission,session).orElseThrow();
 }
 @Transactional
 public Map<String,Object> advance(String id,String session){
  enabled();var args=PlatformRepository.map("id",id,"session",session);
  var rows=repo.jdbc().query("SELECT s.id,s.phase,s.demo_scenario,p.time_limit_ms,p.memory_limit_mb FROM submissions s JOIN problems p ON p.id=s.problem_id WHERE s.public_id=:id AND s.data_kind='DEMO' AND s.origin='local' AND s.demo_session_hash=:session FOR UPDATE",args,(r,n)->PlatformRepository.map("internal",r.getLong(1),"phase",r.getString(2),"scene",r.getString(3),"timeLimit",r.getInt(4),"memoryLimit",r.getInt(5)));
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"提交不存在或无更新权限。");
  var row=rows.getFirst();String phase=(String)row.get("phase"),scene=(String)row.get("scene");args.put("internal",row.get("internal"));
  if(phase.equals("waiting"))repo.jdbc().update("UPDATE submissions SET phase='compiling' WHERE id=:internal",args);
  else if(phase.equals("compiling")&&!Set.of("CE","SystemError").contains(scene))repo.jdbc().update("UPDATE submissions SET phase='judging',verdict='Judging' WHERE id=:internal",args);
  else if(!phase.equals("finished")){
   args.putAll(PlatformRepository.map("scene",scene,"time",time(row,scene),"memory",memory(row,scene),"information",information(scene),"finished",LocalDateTime.now(ZoneOffset.UTC)));
   repo.jdbc().update("UPDATE submissions SET verdict=:scene,phase='finished',finished_at=UTC_TIMESTAMP(6),time_ms=:time,memory_mb=:memory WHERE id=:internal",args);
   repo.jdbc().update("UPDATE submission_details SET information=:information WHERE submission_id=:internal",args);
   if(Set.of("AC","WA").contains(scene))repo.jdbc().update("INSERT INTO submission_cases(submission_id,position,public_name,verdict,time_ms,memory_mb) VALUES(:internal,0,'公开样例结果（固定演示）',:scene,12,2.1)",args);
  }
  return repo.submission(id,session).orElseThrow();
 }
}
