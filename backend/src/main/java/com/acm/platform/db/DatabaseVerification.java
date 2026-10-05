package com.acm.platform.db;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.dao.DataAccessException;
import java.util.*;

@Service @Profile({"mysql-local","aiven"})
public class DatabaseVerification {
 final PlatformRepository repo;final DevelopmentSeed seed;final TransactionTemplate tx;
 public DatabaseVerification(PlatformRepository repo,DevelopmentSeed seed,PlatformTransactionManager manager){this.repo=repo;this.seed=seed;this.tx=new TransactionTemplate(manager);}
 static void check(boolean condition,String label){if(!condition)throw new IllegalStateException("Verification failed: "+label);System.out.println("PASS "+label);}
 void mustReject(String sql,Map<String,?> args,String label){
  try{repo.jdbc().update(sql,args);throw new IllegalStateException("Constraint did not reject: "+label);}
  catch(DataAccessException expected){
   if(!(expected.getMostSpecificCause() instanceof java.sql.SQLException sqlError)||!Set.of(1062,1451,1452,3819).contains(sqlError.getErrorCode()))throw expected;
   System.out.println("PASS "+label);
  }
 }
 @SuppressWarnings("unchecked")
 public void verify(){
  seed.requireDevelopment();
  tx.executeWithoutResult(transaction->{
   transaction.setRollbackOnly();
   String token=UUID.randomUUID().toString(),prefix="QA-"+token,session=DemoService.session(token);
   String code="// 中文代码与换行 😃\nint main() {\n    return 0;\n}\n";
   var args=PlatformRepository.map("public",prefix,"session",session,"code",code);
   repo.jdbc().update("INSERT INTO users(public_id,display_name,is_demo) VALUES(:public,'验证专用用户',TRUE)",args);
   long user=seed.id("users","public_id",prefix);args.put("user",user);
   long category=seed.named("categories",prefix),tag1=seed.named("tags",prefix+"甲"),tag2=seed.named("tags",prefix+"乙");
   args.putAll(PlatformRepository.map("category",category,"body",repo.json(List.of(Map.of("heading","中文标题","paragraphs",List.of("中文正文\n换行 😃"),"code",code))),"preview","[]","article",Math.abs(UUID.randomUUID().getMostSignificantBits()>>>1)));
   repo.jdbc().update("INSERT INTO articles(public_id,author_id,category_id,title,summary,body,preview,status,published_at) VALUES(:article,:user,:category,'验证中文文章','验证摘要',:body,:preview,'PUBLISHED',UTC_TIMESTAMP(6))",args);
   long article=seed.id("articles","public_id",args.get("article"));
   var loaded=repo.article(((Number)args.get("article")).longValue()).orElseThrow();
   check(((List<Map<String,Object>>)loaded.get("sections")).getFirst().get("code").equals(code),"Chinese article and code newline round trip");
   List<String> problemIds=new ArrayList<>();
   for(int n=0;n<2;n++){
    String problem="Q"+token.replace("-","").substring(0,20)+n;problemIds.add(problem);
    args.put("problemPublic",problem);args.put("title",prefix+"题"+n);
    repo.jdbc().update("INSERT INTO problems(public_id,title,difficulty,description,input_format,output_format,constraints_json,template_cpp17,time_limit_ms,memory_limit_mb,source,status,data_kind) VALUES(:problemPublic,:title,'简单','验证题目','输入\n换行','输出\n换行','[]',:code,1000,256,'专用回滚验证','PUBLISHED','DEMO')",args);
    long problemId=seed.id("problems","public_id",problem);args.put("problem",problemId);
    for(int position=0;position<2;position++){
     args.put("tag",position==0?tag1:tag2);args.put("position",position);
     repo.jdbc().update("INSERT INTO problem_tags(problem_id,tag_id,position) VALUES(:problem,:tag,:position)",args);
     repo.jdbc().update("INSERT INTO problem_samples(problem_id,position,input_text,output_text,explanation) VALUES(:problem,:position,'1\n2','3\n','公开多组样例')",args);
    }
    var p=repo.problem(problem).orElseThrow();check(((List<?>)p.get("tags")).size()==2&&((List<?>)p.get("samples")).size()==2,"multi-tag/multi-sample relation "+n);
    for(int attempt=0;attempt<(n==0?2:1);attempt++){
     args.put("submissionPublic",prefix+"-"+n+"-"+attempt);args.put("verdict",n==0?"AC":"WA");
     repo.jdbc().update("INSERT INTO submissions(public_id,user_id,problem_id,language,code,verdict,phase,data_kind,origin,demo_session_hash,demo_scenario,finished_at,time_ms,memory_mb) VALUES(:submissionPublic,:user,:problem,'cpp17',:code,:verdict,'finished','DEMO','local',:session,:verdict,UTC_TIMESTAMP(6),12,2.1)",args);
     long internal=seed.id("submissions","public_id",args.get("submissionPublic"));
     repo.jdbc().update("INSERT INTO submission_details(submission_id,information) VALUES(:id,'验证专用模拟快照')",Map.of("id",internal));
     check(Objects.equals(repo.submission((String)args.get("submissionPublic"),session).orElseThrow().get("code"),code),"immutable source snapshot + user/problem association "+n+"/"+attempt);
     check(!repo.submission((String)args.get("submissionPublic"),"").orElseThrow().containsKey("code"),"other session cannot access source");
    }
   }
   var practice=repo.practice("DEMO",session);check(((Number)practice.get("passed")).intValue()==1,"accepted count deduplicates problem");
   check(((Number)repo.practice("REAL",session).get("passed")).intValue()==0,"demo AC never updates formal practice");
   check(((Number)repo.formalPractice(user).get("passed")).intValue()==0,"formal SQL excludes all demo submissions");
   var formal=PlatformRepository.map("public",prefix+"-real","code",code);
   formal.put("problemPublic","R"+token.replace("-","").substring(0,20));
   repo.jdbc().update("INSERT INTO problems(public_id,title,difficulty,description,input_format,output_format,constraints_json,template_cpp17,time_limit_ms,memory_limit_mb,source,status,data_kind) VALUES(:problemPublic,'正式统计专用回滚题','简单','验证','','','[]',:code,1000,256,'专用回滚验证','PUBLISHED','REAL')",formal);
   formal.put("problem",seed.id("problems","public_id",formal.get("problemPublic")));
   repo.jdbc().update("INSERT INTO users(public_id,display_name) VALUES(:public,'正式统计验证用户')",formal);
   long formalUser=seed.id("users","public_id",formal.get("public"));formal.put("user",formalUser);
   for(int n=0;n<2;n++){formal.put("id",prefix+"-formal-"+n);repo.jdbc().update("INSERT INTO submissions(public_id,user_id,problem_id,language,code,verdict,phase,data_kind,origin,finished_at) VALUES(:id,:user,:problem,'cpp17',:code,'AC','finished','REAL','real',UTC_TIMESTAMP(6))",formal);}
   check(((Number)repo.formalPractice(formalUser).get("passed")).intValue()==1,"formal SQL deduplicates repeated real AC");
   var problems=repo.problems(prefix,"简单",prefix+"甲","","id","asc",2,1,"DEMO",session);check(problems.total()==2&&problems.page()==2&&problems.items().getFirst().get("id").equals(problemIds.get(1)),"database problem filter/sort/pagination");
   var submissions=repo.submissions(problemIds.getFirst(),"cpp17","AC","验证专用用户","mine","time","desc",2,1,"DEMO",session);check(submissions.total()==2&&submissions.items().size()==1&&submissions.page()==2,"database submission filter/sort/pagination");
   mustReject("INSERT INTO users(public_id,display_name) VALUES(:public,'重复用户')",args,"unique public identifier rejects duplicates");
   mustReject("INSERT INTO problem_tags(problem_id,tag_id,position) VALUES(:problem,:tag,:position)",args,"duplicate tag relation rejects duplicates");
   mustReject("INSERT INTO problem_samples(problem_id,position,input_text,output_text,explanation) VALUES(9223372036854775807,0,'','','')",Map.of(),"foreign key rejects nonexistent problem");
   mustReject("DELETE FROM users WHERE id=:user",args,"history user deletion is restricted");
   mustReject("UPDATE submissions SET verdict='Pending',phase='waiting',finished_at=NULL,time_ms=1 WHERE public_id=:submissionPublic",args,"pending submission cannot contain timing metrics");
  });
 }
}
