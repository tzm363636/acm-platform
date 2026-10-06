package com.acm.platform.content;

import com.acm.platform.auth.*;
import com.acm.platform.db.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import java.util.*;
import java.sql.*;
import java.security.SecureRandom;

@Service @Profile({"mysql-local","aiven"}) @Transactional(readOnly=true)
public class ArticleService {
 final PlatformRepository repo;private final SecureRandom random=new SecureRandom();
 public ArticleService(PlatformRepository repo){this.repo=repo;}
 static ResponseStatusException conflict(String s){return new ResponseStatusException(HttpStatus.CONFLICT,s);}
 static void admin(Account a){if(!a.admin())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"仅管理员可执行此操作。");}
 static final String FROM=" FROM articles a JOIN users u ON u.id=a.author_id JOIN categories c ON c.id=a.category_id ";
 Object utc(ResultSet r,String col)throws SQLException{var ts=r.getTimestamp(col,Calendar.getInstance(TimeZone.getTimeZone("UTC")));return ts==null?null:ts.toInstant().toString();}
 Map<String,Object> row(ResultSet r,int n)throws SQLException {
  var m=new LinkedHashMap<String,Object>();for(String f:List.of("public_id","title","summary","status","revision","review_round","category_id"))m.put(switch(f){case "public_id"->"id";case "review_round"->"reviewRound";case "category_id"->"categoryId";default->f;},r.getObject(f));
  long id=r.getLong("id");m.put("author",r.getString("author"));m.put("authorId",r.getLong("author_id"));m.put("category",r.getString("category"));m.put("featured",r.getBoolean("featured"));m.put("wide",r.getBoolean("wide"));
  for(String col:List.of("submitted_at","published_at","updated_at"))m.put(switch(col){case "submitted_at"->"submittedAt";case "published_at"->"publishedAt";default->"updatedAt";},utc(r,col));
  m.put("tagIds",repo.jdbc().queryForList("SELECT tag_id FROM article_tags WHERE article_id=:id ORDER BY position",Map.of("id",id),Long.class));
  try{m.put("sections",repo.mapper().readValue(r.getString("body"),Object.class));}catch(Exception e){throw new IllegalStateException("Invalid stored article body.");}
  m.put("reviews",repo.jdbc().query("SELECT ar.decision,ar.reason,ar.review_round,ar.reviewed_at,u.display_name reviewer FROM article_reviews ar JOIN users u ON u.id=ar.reviewer_id WHERE ar.article_id=:id ORDER BY ar.review_round DESC",Map.of("id",id),(rs,k)->{var v=new LinkedHashMap<String,Object>();v.put("decision",rs.getString(1));v.put("reason",rs.getString(2));v.put("round",rs.getInt(3));v.put("reviewedAt",utc(rs,"reviewed_at"));v.put("reviewer",rs.getString(5));return v;}));return m;
 }
 String select(){return "SELECT a.*,u.display_name author,c.name category"+FROM;}
 public Map<String,Object> detail(Account user,long id){
  var result=repo.jdbc().query(select()+" WHERE a.public_id=:id",Map.of("id",id),this::row).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"文章不存在。"));
  if(!user.admin()&&((Number)result.get("authorId")).longValue()!=user.id())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"只能查看自己的投稿。");return result;
 }
 public Page<Map<String,Object>> list(Account user,boolean all,String q,String status,int page,int size){return list(user,all,q,status,page,size,"updated","desc");}
 public Page<Map<String,Object>> list(Account user,boolean all,String q,String status,int page,int size,String sort,String direction){
  if(all)admin(user);var args=new HashMap<String,Object>();args.put("user",user.id());args.put("q",PlatformRepository.like(q));args.put("status",status);
  String where=all?" WHERE a.data_kind='REAL'":" WHERE a.author_id=:user AND a.data_kind='REAL'";
  if(!status.isEmpty()){if(!Set.of("DRAFT","PENDING","PUBLISHED","REJECTED","ARCHIVED").contains(status))throw AuthService.bad("无效文章状态。");where+=" AND a.status=:status";}
  if(!q.isBlank())where+=" AND (a.title LIKE :q ESCAPE '!' OR u.display_name LIKE :q ESCAPE '!')";
  long total=repo.jdbc().queryForObject("SELECT COUNT(*)"+FROM+where,args,Long.class);size=Page.size(size);page=Page.current(page,total,size);args.put("limit",size);args.put("offset",(page-1)*size);
  String ordered=PlatformRepository.order(sort,direction,Map.of("updated","a.updated_at","submitted","a.submitted_at"));
  String summary="SELECT a.public_id,a.title,a.summary,a.status,a.featured,u.display_name author,c.name category,a.submitted_at,a.published_at,a.updated_at,CASE WHEN a.status='REJECTED' THEN ar.reason ELSE NULL END rejection_reason";
  return Page.of(repo.jdbc().query(summary+FROM+" LEFT JOIN article_reviews ar ON ar.article_id=a.id AND ar.review_round=a.review_round AND ar.decision='REJECTED'"+where+" ORDER BY "+ordered+",a.id "+direction+" LIMIT :limit OFFSET :offset",args,(r,n)->{
   var m=new LinkedHashMap<String,Object>();m.put("id",r.getLong("public_id"));for(String f:List.of("title","summary","status","author","category"))m.put(f,r.getString(f));m.put("featured",r.getBoolean("featured"));m.put("rejectionReason",r.getString("rejection_reason"));
   m.put("submittedAt",utc(r,"submitted_at"));m.put("publishedAt",utc(r,"published_at"));m.put("updatedAt",utc(r,"updated_at"));return m;
  }),page,size,total);
 }
 public Map<String,Object> options(){return Map.of("categories",repo.jdbc().queryForList("SELECT id,name FROM categories ORDER BY name",Map.of()),"tags",repo.jdbc().queryForList("SELECT id,name FROM tags ORDER BY name",Map.of()));}
 private Map<String,Object> lock(Account user,long publicId,int revision){
  var a=repo.jdbc().queryForList("SELECT id,author_id,status,revision,review_round FROM articles WHERE public_id=:id FOR UPDATE",Map.of("id",publicId)).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"文章不存在。"));
  if(!user.admin()&&((Number)a.get("author_id")).longValue()!=user.id())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"不能修改其他用户的文章。");
  if(((Number)a.get("revision")).intValue()!=revision)throw conflict("文章已经变化，请重新加载后再操作；当前输入已保留。");return a;
 }
 private String json(Object obj){try{return repo.mapper().writeValueAsString(obj);}catch(Exception e){throw AuthService.bad("正文格式无效。");}}
 private void relations(long id,ArticleInput in){
  if(repo.jdbc().queryForObject("SELECT COUNT(*) FROM categories WHERE id=:id",Map.of("id",in.categoryId()),Long.class)!=1)throw AuthService.bad("分类不存在。");
  repo.jdbc().update("DELETE FROM article_tags WHERE article_id=:id",Map.of("id",id));int index=0;
  for(long tag:in.tagIds()){if(repo.jdbc().queryForObject("SELECT COUNT(*) FROM tags WHERE id=:id",Map.of("id",tag),Long.class)!=1)throw AuthService.bad("标签不存在。");repo.jdbc().update("INSERT INTO article_tags(article_id,tag_id,position) VALUES(:id,:tag,:position)",Map.of("id",id,"tag",tag,"position",index++));}
 }
 private Map<String,Object> fields(ArticleInput in){in.validate();if(repo.jdbc().queryForObject("SELECT COUNT(*) FROM categories WHERE id=:id",Map.of("id",in.categoryId()),Long.class)!=1)throw AuthService.bad("分类不存在。");var args=new HashMap<String,Object>();args.put("title",in.title().strip());args.put("summary",in.summary().strip());args.put("category",in.categoryId());args.put("body",json(in.sections()));var code=in.sections().stream().map(ArticleInput.Section::code).filter(x->x!=null&&!x.isBlank()).findFirst().orElse("// "+in.title());args.put("preview",json(code.lines().limit(5).toList()));return args;}
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> create(Account user,ArticleInput in){
  var args=fields(in);args.put("draftKey",in.draftKey());args.put("author",user.id());
  if(in.draftKey()!=null){var existing=repo.jdbc().queryForList("SELECT public_id FROM articles WHERE author_id=:author AND draft_key=:draftKey",args,Long.class);if(!existing.isEmpty())return detail(user,existing.getFirst());}
  long publicId=random.nextLong(1_000_000_000_000L,8_000_000_000_000_000L);args.put("public",publicId);var key=new GeneratedKeyHolder();
  try{repo.jdbc().update("INSERT INTO articles(public_id,author_id,category_id,title,summary,body,preview,status,data_kind,draft_key) VALUES(:public,:author,:category,:title,:summary,:body,:preview,'DRAFT','REAL',:draftKey)",new MapSqlParameterSource(args),key,new String[]{"id"});}
  catch(org.springframework.dao.DuplicateKeyException e){if(in.draftKey()==null)throw e;var existing=repo.jdbc().queryForList("SELECT public_id FROM articles WHERE author_id=:author AND draft_key=:draftKey",args,Long.class);if(existing.isEmpty())throw e;return detail(user,existing.getFirst());}
  relations(Objects.requireNonNull(key.getKey()).longValue(),in);return detail(user,publicId);
 }
 @Transactional public Map<String,Object> save(Account user,long id,ArticleInput in){
  if(in.revision()==null)throw AuthService.bad("缺少文章版本。");var a=lock(user,id,in.revision());String status=(String)a.get("status");
  if(status.equals("PENDING"))throw conflict("待审核文章不能编辑，请先撤回。");if(!user.admin()&&!Set.of("DRAFT","REJECTED").contains(status))throw conflict("已发布或下架文章需要管理员处理。");
  var args=fields(in);args.put("id",a.get("id"));repo.jdbc().update("UPDATE articles SET title=:title,summary=:summary,category_id=:category,body=:body,preview=:preview,revision=revision+1 WHERE id=:id",args);relations(((Number)a.get("id")).longValue(),in);return detail(user,id);
 }
 public record Action(int revision,String reason){}
 private void audit(long id,int round,Account admin,String decision,String reason){repo.jdbc().update("INSERT INTO article_reviews(article_id,review_round,reviewer_id,decision,reason,reviewed_at) VALUES(:id,:round,:reviewer,:decision,:reason,UTC_TIMESTAMP(6))",Map.of("id",id,"round",round,"reviewer",admin.id(),"decision",decision,"reason",reason));}
 @Transactional public Map<String,Object> transition(Account user,long id,String action,Action input){
  if(Set.of("approve","reject","publish","archive").contains(action))admin(user);
  var a=lock(user,id,input.revision());String before=(String)a.get("status"),next;int round=((Number)a.get("review_round")).intValue();String reason=input.reason()==null?"":input.reason().strip();
  if(!reason.isEmpty())reason=ArticleInput.plain(reason,1,2000,"操作原因");
  switch(action){
   case "submit"->{if(!Set.of("DRAFT","REJECTED").contains(before))throw conflict("只有草稿或驳回文章可以提交审核。");next="PENDING";round++;}
   case "withdraw"->{if(!before.equals("PENDING"))throw conflict("投稿已处理或已撤回。");next="DRAFT";}
   case "approve","reject"->{if(!before.equals("PENDING"))throw conflict("该投稿已处理，请刷新列表。");next=action.equals("approve")?"PUBLISHED":"REJECTED";if(action.equals("reject"))reason=ArticleInput.plain(reason,1,2000,"驳回原因");audit(((Number)a.get("id")).longValue(),round,user,action.equals("approve")?"APPROVED":"REJECTED",reason);}
   case "publish"->{if(!Set.of("DRAFT","REJECTED","ARCHIVED").contains(before))throw conflict("当前状态不能直接发布；待审投稿请使用批准操作。");next="PUBLISHED";round++;audit(((Number)a.get("id")).longValue(),round,user,"PUBLISHED",reason);}
   case "archive"->{if(!before.equals("PUBLISHED"))throw conflict("只有已发布文章可以下架。");next="ARCHIVED";round++;audit(((Number)a.get("id")).longValue(),round,user,"ARCHIVED",reason);}
   default->throw AuthService.bad("不支持的文章操作。");
  }
  repo.jdbc().update("UPDATE articles SET status=:status,review_round=:round,revision=revision+1,submitted_at=IF(:status='PENDING',UTC_TIMESTAMP(6),submitted_at),published_at=IF(:status='PUBLISHED',UTC_TIMESTAMP(6),published_at) WHERE id=:id",Map.of("status",next,"round",round,"id",a.get("id")));return detail(user,id);
 }
 public record Flags(int revision,boolean featured,boolean wide){}
 @Transactional public Object flags(Account user,long id,Flags flags){admin(user);var a=lock(user,id,flags.revision());repo.jdbc().update("UPDATE articles SET featured=:featured,wide=:wide,revision=revision+1 WHERE id=:id",Map.of("id",a.get("id"),"featured",flags.featured(),"wide",flags.wide()));return detail(user,id);}
 public Page<Map<String,Object>> users(Account user,String q,int page,int size){admin(user);var args=new HashMap<String,Object>();args.put("q",PlatformRepository.like(q));String where=" WHERE username IS NOT NULL AND is_demo=FALSE";if(!q.isBlank())where+=" AND (username LIKE :q ESCAPE '!' OR display_name LIKE :q ESCAPE '!')";long total=repo.jdbc().queryForObject("SELECT COUNT(*) FROM users"+where,args,Long.class);size=Page.size(size);page=Page.current(page,total,size);args.put("limit",size);args.put("offset",(page-1)*size);return Page.of(repo.jdbc().query("SELECT id,username,display_name,role,created_at,disabled_at FROM users"+where+" ORDER BY id DESC LIMIT :limit OFFSET :offset",args,(r,n)->{var m=new LinkedHashMap<String,Object>();m.put("id",r.getLong(1));m.put("username",r.getString(2));m.put("displayName",r.getString(3));m.put("role",r.getString(4));m.put("createdAt",utc(r,"created_at"));m.put("disabledAt",utc(r,"disabled_at"));return m;}),page,size,total);}
 public Object statistics(Account user){admin(user);return Map.of("users",repo.jdbc().queryForObject("SELECT COUNT(*) FROM users WHERE username IS NOT NULL AND is_demo=FALSE",Map.of(),Long.class),"articles",repo.jdbc().queryForList("SELECT status,COUNT(*) count FROM articles WHERE data_kind='REAL' GROUP BY status",Map.of()));}
 public record Name(String name){}
 @Transactional public Object taxonomy(Account user,String kind,Long id,Name input){admin(user);String table=switch(kind){case "categories"->"categories";case "tags"->"tags";default->throw AuthService.bad("无效分类类型。");};String name=ArticleInput.plain(input.name(),1,64,"名称");
  try{if(id==null)repo.jdbc().update("INSERT INTO "+table+"(name) VALUES(:name)",Map.of("name",name));else if(repo.jdbc().update("UPDATE "+table+" SET name=:name WHERE id=:id",Map.of("id",id,"name",name))!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"条目不存在。");}catch(org.springframework.dao.DuplicateKeyException e){throw conflict("名称已存在。");}return options();
 }
}
