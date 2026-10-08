package com.acm.platform.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import javax.sql.DataSource;
import java.sql.*;
import java.time.*;
import java.util.*;

@Repository @Profile({"mysql-local","aiven"})
@Transactional(readOnly=true)
public class PlatformRepository {
 private final NamedParameterJdbcTemplate jdbc;
 private final ObjectMapper json;
 public PlatformRepository(DataSource source,ObjectMapper json){this.jdbc=new NamedParameterJdbcTemplate(source);this.json=json;}
 public NamedParameterJdbcTemplate jdbc(){return jdbc;}
 public ObjectMapper mapper(){return json;}
 public static String like(String value){return "%"+value.strip().replace("!","!!").replace("%","!%").replace("_","!_")+"%";}
 public static String order(String field,String direction,Map<String,String> allowed){
  String column=allowed.get(field);
  if(column==null || !Set.of("asc","desc").contains(direction)) throw bad("无效排序条件。");
  return column+" "+direction.toUpperCase(Locale.ROOT);
 }
 public static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 static Map<String,Object> map(Object... values){Map<String,Object> result=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2) result.put((String)values[i],values[i+1]);return result;}
 Object parse(String value){try{return json.readValue(value,new TypeReference<Object>(){});}catch(Exception e){throw new IllegalStateException("Stored content format is invalid.");}}
 String json(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw bad("内容格式错误。");}}
 static Object utc(ResultSet rs,String column) throws SQLException {Timestamp ts=rs.getTimestamp(column,Calendar.getInstance(TimeZone.getTimeZone("UTC"))); return ts==null?null:ts.toInstant().toString();}
 static Number nullableNumber(ResultSet rs,String column) throws SQLException {return (Number)rs.getObject(column);}
 long count(String sql,Map<String,?> params){return Optional.ofNullable(jdbc.queryForObject(sql,params,Long.class)).orElse(0L);}
 List<String> tags(String table,String owner,long id){return jdbc.queryForList("SELECT t.name FROM "+table+" x JOIN tags t ON t.id=x.tag_id WHERE x."+owner+"=:id ORDER BY x.position",Map.of("id",id),String.class);}
 Map<String,Object> articleRow(ResultSet rs,int row) throws SQLException {
  long internal=rs.getLong("id");
  return map("id",rs.getLong("public_id"),"title",rs.getString("title"),"summary",rs.getString("summary"),"category",rs.getString("category"),
   "tags",tags("article_tags","article_id",internal),"preview",parse(rs.getString("preview")),"sections",parse(rs.getString("body")),
   "featured",rs.getBoolean("featured"),"wide",rs.getBoolean("wide"),"author",rs.getString("author"),"authorProfile","site-author".equals(rs.getString("author_public_id"))?"./author.html":null,"publishedAt",utc(rs,"published_at"),"updatedAt",utc(rs,"updated_at"));
 }
 static final String ARTICLE_FROM=" FROM articles a JOIN categories c ON c.id=a.category_id JOIN users u ON u.id=a.author_id ";
 @Transactional(readOnly=true)
 public Page<Map<String,Object>> articles(String q,String category,String tag,String layout,int page,int size){
  return articles(q,category,tag,layout,page,size,"id");
 }
 public Page<Map<String,Object>> articles(String q,String category,String tag,String layout,int page,int size,String sort){
  String ordered=switch(sort){
   case "id" -> "a.public_id ASC";
   case "recommended" -> "a.featured DESC,a.wide DESC,a.public_id ASC";
   case "published" -> "a.published_at DESC,a.id DESC";
   case "updated" -> "a.updated_at DESC,a.id DESC";
   default -> throw bad("无效文章排序条件。");
  };
  Map<String,Object> args=map("q",like(q),"category",category,"tag",tag);
  String where=" WHERE a.status='PUBLISHED'";
  if(!q.isBlank()) where+=" AND (a.title LIKE :q ESCAPE '!' OR a.summary LIKE :q ESCAPE '!' OR c.name LIKE :q ESCAPE '!' OR EXISTS(SELECT 1 FROM article_tags atg JOIN tags t ON t.id=atg.tag_id WHERE atg.article_id=a.id AND t.name LIKE :q ESCAPE '!'))";
  if(!category.isBlank()) where+=" AND c.name=:category";
  if(!tag.isBlank()) where+=" AND EXISTS(SELECT 1 FROM article_tags x JOIN tags t ON t.id=x.tag_id WHERE x.article_id=a.id AND t.name=:tag)";
  where+=switch(layout){case "featured"->" AND a.featured=TRUE";case "wide"->" AND a.wide=TRUE AND a.featured=FALSE";case "regular"->" AND a.featured=FALSE AND a.wide=FALSE";case ""->"";default->throw bad("无效文章布局筛选。");};
  long total=count("SELECT COUNT(*)"+ARTICLE_FROM+where,args);size=Page.size(size);page=Page.current(page,total,size);
  args.put("limit",size);args.put("offset",(page-1)*size);
  // Never read body or review history for a public list. Fetch this page's tags in one query.
  List<Long> ids=new ArrayList<>();
  var items=jdbc.query("SELECT a.id,a.public_id,a.title,a.summary,a.preview,a.featured,a.wide,a.published_at,a.updated_at,c.name category,u.display_name author,u.public_id author_public_id"+ARTICLE_FROM+where+" ORDER BY "+ordered+" LIMIT :limit OFFSET :offset",args,(rs,n)->{
   ids.add(rs.getLong("id"));
   return map("id",rs.getLong("public_id"),"title",rs.getString("title"),"summary",rs.getString("summary"),"category",rs.getString("category"),"preview",parse(rs.getString("preview")),"featured",rs.getBoolean("featured"),"wide",rs.getBoolean("wide"),"author",rs.getString("author"),"authorProfile","site-author".equals(rs.getString("author_public_id"))?"./author.html":null,"publishedAt",utc(rs,"published_at"),"updatedAt",utc(rs,"updated_at"));
  });
  Map<Long,List<String>> grouped=new HashMap<>();
  if(!ids.isEmpty())jdbc.query("SELECT x.article_id,t.name FROM article_tags x JOIN tags t ON t.id=x.tag_id WHERE x.article_id IN (:ids) ORDER BY x.article_id,x.position",Map.of("ids",ids),(rs,n)->{grouped.computeIfAbsent(rs.getLong(1),k->new ArrayList<>()).add(rs.getString(2));return 0;});
  for(int i=0;i<items.size();i++)items.get(i).put("tags",grouped.getOrDefault(ids.get(i),List.of()));
  return Page.of(items,page,size,total);
 }
 public Optional<Map<String,Object>> article(long id){return jdbc.query("SELECT a.*,c.name category,u.display_name author,u.public_id author_public_id"+ARTICLE_FROM+" WHERE a.public_id=:id AND a.status='PUBLISHED'",Map.of("id",id),this::articleRow).stream().findFirst();}
 public Map<String,Object> articleOptions(){
  var counts=jdbc.query("SELECT c.name,COUNT(*) total FROM categories c JOIN articles a ON a.category_id=c.id WHERE a.status='PUBLISHED' GROUP BY c.id,c.name ORDER BY c.name",Map.of(),(r,n)->map("name",r.getString(1),"total",r.getLong(2)));
  return map("categories",counts.stream().map(c->c.get("name")).toList(),"tags",jdbc.queryForList("SELECT DISTINCT t.name FROM tags t JOIN article_tags x ON x.tag_id=t.id JOIN articles a ON a.id=x.article_id WHERE a.status='PUBLISHED' ORDER BY t.name",Map.of(),String.class),"publishedTotal",counts.stream().mapToLong(c->((Number)c.get("total")).longValue()).sum(),"categoryCount",counts.size());
 }
 // One read transaction keeps the page and public metadata consistent. No shared/private cache.
 public Object articleFeed(String q,String category,String tag,String layout,int page,int size,String sort){
  var result=articles(q,category,tag,layout,page,size,sort);
  return map("items",result.items(),"page",result.page(),"pages",result.pages(),"start",result.start(),"end",result.end(),"total",result.total(),"size",result.size(),"options",articleOptions());
 }
 Map<String,Object> problemRow(ResultSet rs,int row) throws SQLException {
  long internal=rs.getLong("id");
  return map("id",rs.getString("public_id"),"title",rs.getString("title"),"difficulty",rs.getString("difficulty"),"tags",tags("problem_tags","problem_id",internal),
   "timeLimit",rs.getInt("time_limit_ms"),"memoryLimit",rs.getInt("memory_limit_mb"),"source",rs.getString("source"),"description",rs.getString("description"),"input",rs.getString("input_format"),"output",rs.getString("output_format"),
   "samples",jdbc.query("SELECT input_text,output_text,explanation FROM problem_samples WHERE problem_id=:id ORDER BY position",Map.of("id",internal),(r,n)->map("input",r.getString(1),"output",r.getString(2),"explanation",r.getString(3))),
   "constraints",parse(rs.getString("constraints_json")),"template",rs.getString("template_cpp17"),"articleIds",jdbc.queryForList("SELECT a.public_id FROM problem_articles x JOIN articles a ON a.id=x.article_id WHERE x.problem_id=:id AND a.status='PUBLISHED' ORDER BY x.position",Map.of("id",internal),Long.class));
 }
 static final String PERSONAL="CASE WHEN COALESCE(ps.accepted,0)>0 THEN 'passed' WHEN COALESCE(ps.attempted,0)>0 THEN 'failed' ELSE 'untried' END";
 static final String PROBLEM_FROM=" FROM problems p LEFT JOIN (SELECT problem_id,COUNT(*) submissions,SUM(verdict='AC') accepted,SUM(verdict NOT IN ('Pending','Judging','SystemError')) judged FROM submissions WHERE data_kind=:kind GROUP BY problem_id) st ON st.problem_id=p.id LEFT JOIN (SELECT problem_id,SUM(verdict='AC') accepted,SUM(verdict NOT IN ('Pending','Judging','SystemError')) attempted FROM submissions WHERE data_kind=:kind AND data_kind='DEMO' AND demo_session_hash=:session GROUP BY problem_id) ps ON ps.problem_id=p.id ";
 @Transactional(readOnly=true)
 public Page<Map<String,Object>> problems(String q,String difficulty,String tag,String status,String sort,String direction,int page,int size,String kind,String session){
  Map<String,Object> args=map("q",like(q),"difficulty",difficulty,"tag",tag,"status",status,"kind",kind,"session",session);
  String where=" WHERE p.status='PUBLISHED' AND p.data_kind=:kind";
  if(!q.isBlank()) where+=" AND (p.public_id LIKE :q ESCAPE '!' OR p.title LIKE :q ESCAPE '!')";
  if(!difficulty.isBlank()) where+=" AND p.difficulty=:difficulty";
  if(!tag.isBlank()) where+=" AND EXISTS(SELECT 1 FROM problem_tags x JOIN tags t ON t.id=x.tag_id WHERE x.problem_id=p.id AND t.name=:tag)";
  if(!status.isBlank()) {if(!Set.of("untried","failed","passed").contains(status))throw bad("无效个人状态。");if(session.isEmpty()||!kind.equals("DEMO"))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"当前列表只支持演示个人状态；真实状态接口尚未开放。");where+=" AND "+PERSONAL+"=:status";}
  String ordered=order(sort,direction,Map.of("id","p.public_id","difficulty","FIELD(p.difficulty,'简单','中等','困难')","passRate","COALESCE(100.0*st.accepted/NULLIF(st.judged,0),-1)","status","CASE WHEN COALESCE(ps.accepted,0)>0 THEN 2 WHEN COALESCE(ps.attempted,0)>0 THEN 1 ELSE 0 END"));
  long total=count("SELECT COUNT(*)"+PROBLEM_FROM+where,args);size=Page.size(size);page=Page.current(page,total,size);args.put("limit",size);args.put("offset",(page-1)*size);
  var items=jdbc.query("SELECT p.*,COALESCE(st.submissions,0) submission_count,100.0*st.accepted/NULLIF(st.judged,0) pass_rate,"+PERSONAL+" personal_status"+PROBLEM_FROM+where+" ORDER BY "+ordered+",p.public_id LIMIT :limit OFFSET :offset",args,(rs,n)->{
   var item=problemRow(rs,n);item.put("statistics",map("submissions",rs.getLong("submission_count"),"passRate",nullableNumber(rs,"pass_rate")));item.put("personalStatus",rs.getString("personal_status"));return item;
  });return Page.of(items,page,size,total);
 }
 public Optional<Map<String,Object>> problem(String id){return jdbc.query("SELECT * FROM problems WHERE public_id=:id AND status='PUBLISHED'",Map.of("id",id),this::problemRow).stream().findFirst();}
 public Map<String,Object> options(){return map("tags",jdbc.queryForList("SELECT DISTINCT t.name FROM tags t JOIN problem_tags x ON x.tag_id=t.id JOIN problems p ON p.id=x.problem_id WHERE p.status='PUBLISHED' ORDER BY t.name",Map.of(),String.class),"users",jdbc.queryForList("SELECT DISTINCT u.display_name FROM users u JOIN submissions s ON s.user_id=u.id WHERE s.data_kind='DEMO' ORDER BY u.display_name",Map.of(),String.class));}
 @Transactional(readOnly=true)
 public Map<String,Object> practice(String kind,String session){
  long total=count("SELECT COUNT(*) FROM problems WHERE status='PUBLISHED' AND data_kind=:kind",Map.of("kind",kind));
  if(session.isEmpty() || !kind.equals("DEMO")) return map("passed",0,"total",total,"recent",0,"review",null,"identityAvailable",false);
  Map<String,Object> args=map("session",session);
  long passed=count("SELECT COUNT(DISTINCT problem_id) FROM submissions WHERE data_kind='DEMO' AND demo_session_hash=:session AND verdict='AC'",args);
  long recent=count("SELECT COUNT(DISTINCT problem_id) FROM submissions WHERE data_kind='DEMO' AND demo_session_hash=:session AND verdict='AC' AND finished_at>=UTC_TIMESTAMP(6)-INTERVAL 7 DAY",args);
  long review=count("SELECT COUNT(*) FROM (SELECT problem_id FROM submissions WHERE data_kind='DEMO' AND demo_session_hash=:session GROUP BY problem_id HAVING SUM(verdict='AC')=0 AND SUM(verdict NOT IN ('Pending','Judging','SystemError'))>0) x",args);
  return map("passed",passed,"total",total,"recent",recent,"review",review,"identityAvailable",true);
 }
 // Reserved for a future authenticated server principal; never accepts a browser-supplied user ID.
 public Map<String,Object> formalPractice(long trustedUserId){
  var args=Map.of("user",trustedUserId);
  return map("passed",count("SELECT COUNT(DISTINCT s.problem_id) FROM submissions s JOIN problems p ON p.id=s.problem_id WHERE s.user_id=:user AND s.data_kind='REAL' AND p.data_kind='REAL' AND p.status='PUBLISHED' AND s.verdict='AC'",args),
    "total",count("SELECT COUNT(*) FROM problems WHERE status='PUBLISHED' AND data_kind='REAL'",Map.of()),
    "recent",count("SELECT COUNT(DISTINCT s.problem_id) FROM submissions s JOIN problems p ON p.id=s.problem_id WHERE s.user_id=:user AND s.data_kind='REAL' AND p.data_kind='REAL' AND p.status='PUBLISHED' AND s.verdict='AC' AND s.finished_at>=UTC_TIMESTAMP(6)-INTERVAL 7 DAY",args));
 }
 static final String SUBMISSION_FROM=" FROM submissions s JOIN problems p ON p.id=s.problem_id JOIN users u ON u.id=s.user_id LEFT JOIN submission_details d ON d.submission_id=s.id ";
 Map<String,Object> submission(ResultSet rs,int row,String session,boolean detail) throws SQLException{
  long id=rs.getLong("id");var result=map("id",rs.getString("public_id"),"problemId",rs.getString("problem_public_id"),"problemTitle",rs.getString("problem_title"),"user",rs.getString("display_name"),"language",rs.getString("language"),"verdict",rs.getString("verdict"),"phase",rs.getString("phase"),"submittedAt",utc(rs,"submitted_at"),"finishedAt",utc(rs,"finished_at"),"timeMs",nullableNumber(rs,"time_ms"),"memoryMB",nullableNumber(rs,"memory_mb"),"information",detail?rs.getString("information"):"", "scenario",rs.getString("demo_scenario"),"origin",rs.getString("origin"),"dataKind",rs.getString("data_kind"),"compilerVersion",rs.getString("compiler_version"),"cases",List.of());
  if(detail){
   if(!session.isBlank() && session.equals(rs.getString("demo_session_hash")) && "DEMO".equals(rs.getString("data_kind"))) result.put("code",rs.getString("code"));
   result.put("cases",jdbc.query("SELECT public_name,verdict,time_ms,memory_mb FROM submission_cases WHERE submission_id=:id ORDER BY position",Map.of("id",id),(r,n)->map("name",r.getString(1),"verdict",r.getString(2),"timeMs",nullableNumber(r,"time_ms"),"memoryMB",nullableNumber(r,"memory_mb"))));
  }return result;
 }
 static final String SUBMISSION_SELECT="SELECT s.*,p.public_id problem_public_id,p.title problem_title,u.display_name,d.information,d.compiler_version";
 @Transactional(readOnly=true)
 public Page<Map<String,Object>> submissions(String q,String language,String verdict,String user,String scope,String sort,String direction,int page,int size,String kind,String session){
  Map<String,Object> args=map("q",like(q),"language",language,"verdict",verdict,"user",user,"kind",kind,"session",session);
  String where=" WHERE s.data_kind=:kind";
  if(scope.equals("mine")){if(session.isBlank()||!kind.equals("DEMO")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"当前列表只支持演示个人记录；真实记录接口尚未开放。");where+=" AND s.demo_session_hash=:session";}else if(!scope.equals("all"))throw bad("无效记录范围。");
  if(!q.isBlank())where+=" AND (p.public_id LIKE :q ESCAPE '!' OR p.title LIKE :q ESCAPE '!')";
  if(!language.isBlank())where+=" AND s.language=:language";
  if(!verdict.isBlank())where+=" AND s.verdict=:verdict";
  if(!user.isBlank())where+=" AND u.display_name=:user";
  String ordered=order(sort,direction,Map.of("id","s.public_id","time","s.submitted_at"));
  long total=count("SELECT COUNT(*)"+SUBMISSION_FROM+where,args);size=Page.size(size);page=Page.current(page,total,size);args.put("limit",size);args.put("offset",(page-1)*size);
  return Page.of(jdbc.query(SUBMISSION_SELECT+SUBMISSION_FROM+where+" ORDER BY "+ordered+",s.id "+direction+" LIMIT :limit OFFSET :offset",args,(rs,n)->submission(rs,n,session,false)),page,size,total);
 }
 public Optional<Map<String,Object>> submission(String id,String session){return jdbc.query(SUBMISSION_SELECT+SUBMISSION_FROM+" WHERE s.public_id=:id",Map.of("id",id),(rs,n)->submission(rs,n,session,true)).stream().findFirst();}
}
