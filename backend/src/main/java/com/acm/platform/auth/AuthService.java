package com.acm.platform.auth;

import com.acm.platform.db.PlatformRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.sql.*;
import java.nio.charset.StandardCharsets;

@Service @Profile({"mysql-local","aiven"}) @Transactional(readOnly=true)
public class AuthService {
 private final PlatformRepository repo;private final PasswordEncoder encoder;private final String dummyHash;
 public AuthService(PlatformRepository repo,PasswordEncoder encoder){this.repo=repo;this.encoder=encoder;dummyHash=encoder.encode(UUID.randomUUID().toString());}
 public record Register(String username,String displayName,String password){}
 public record Login(String username,String password){}
 public static String username(String name){if(name==null||!name.matches("[A-Za-z0-9_]{3,32}"))throw bad("用户名需要 3–32 位英文字母、数字或下划线。");return name.toLowerCase(Locale.ROOT);}
 public static String text(String text,int min,int max,String field){if(text==null||text.strip().length()<min||text.length()>max||text.indexOf('\0')>=0)throw bad(field+"长度或内容无效。");return text.strip();}
 public static void password(String value){if(value==null||value.length()<12||value.getBytes(StandardCharsets.UTF_8).length>72||value.indexOf('\0')>=0)throw bad("密码至少 12 个字符，UTF-8 编码不超过 72 字节。");}
 public static ResponseStatusException bad(String msg){return new ResponseStatusException(HttpStatus.BAD_REQUEST,msg);}
 private Account row(ResultSet r,int n)throws SQLException{return new Account(r.getLong("id"),r.getString("username"),r.getString("display_name"),r.getString("role"),r.getTimestamp("created_at",Calendar.getInstance(TimeZone.getTimeZone("UTC"))).toInstant().toString(),r.getLong("credential_version"));}
 public Account account(long id){return repo.jdbc().query("SELECT id,username,display_name,role,created_at,credential_version FROM users WHERE id=:id AND username IS NOT NULL AND is_demo=FALSE AND disabled_at IS NULL",Map.of("id",id),this::row).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"会话已失效，请重新登录。"));}
 public Account current(Authentication auth){if(auth==null||!(auth.getPrincipal() instanceof Account a))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"请先登录。");
  var attrs=org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
  Account fresh=attrs instanceof org.springframework.web.context.request.ServletRequestAttributes request&&request.getRequest().getAttribute("acm.current-account") instanceof Account cached?cached:account(a.id());
  if(a.credentialVersion()!=fresh.credentialVersion())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"登录凭据已更新，请重新登录。");return fresh;
 }
 @Transactional public Account register(Register r){String name=username(r.username());password(r.password());String display=text(r.displayName(),1,100,"显示名称");
  try{repo.jdbc().update("INSERT INTO users(public_id,username,display_name,password_hash,role,is_demo) VALUES(:public,:name,:display,:hash,'USER',FALSE)",Map.of("public","account-"+UUID.randomUUID(),"name",name,"display",display,"hash",encoder.encode(r.password())));}catch(DuplicateKeyException e){throw new ResponseStatusException(HttpStatus.CONFLICT,"该用户名不可用，请选择其他用户名。");}
  return byName(name);
 }
 private Account byName(String name){return repo.jdbc().query("SELECT id,username,display_name,role,created_at,credential_version FROM users WHERE username=:name AND is_demo=FALSE AND disabled_at IS NULL",Map.of("name",name),this::row).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"用户名或密码错误。"));}
 public Account authenticate(Login r){String name=r.username()==null?"":r.username().toLowerCase(Locale.ROOT);String pass=r.password()==null?"":r.password();
  var hashes=repo.jdbc().queryForList("SELECT password_hash FROM users WHERE username=:name AND is_demo=FALSE AND disabled_at IS NULL",Map.of("name",name),String.class);
  String hash=hashes.isEmpty()?dummyHash:hashes.getFirst();boolean matched=pass.getBytes(StandardCharsets.UTF_8).length<=72&&encoder.matches(pass,hash);
  if(!matched||hashes.isEmpty())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"用户名或密码错误。");return byName(name);
 }
 public record Profile(String displayName){}
 public record PasswordChange(String currentPassword,String newPassword,String confirmPassword){}
 @Transactional public Account updateProfile(Account user,Profile input){String display=text(input.displayName(),1,100,"显示名称");if(java.util.regex.Pattern.compile("<\\s*/?\\s*[a-zA-Z][^>]*>").matcher(display).find())throw bad("显示名称请使用纯文本。");repo.jdbc().update("UPDATE users SET display_name=:name WHERE id=:id",Map.of("name",display,"id",user.id()));return account(user.id());}
 @Transactional public void changePassword(Account user,PasswordChange input){
  password(input.newPassword());if(!Objects.equals(input.newPassword(),input.confirmPassword()))throw bad("两次新密码输入不一致。");
  var record=repo.jdbc().queryForMap("SELECT password_hash,credential_version FROM users WHERE id=:id AND disabled_at IS NULL FOR UPDATE",Map.of("id",user.id()));
  if(((Number)record.get("credential_version")).longValue()!=user.credentialVersion())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"会话已失效，请重新登录。");
  String old=input.currentPassword()==null?"":input.currentPassword(),hash=(String)record.get("password_hash");
  if(old.getBytes(StandardCharsets.UTF_8).length>72||!encoder.matches(old,hash))throw bad("当前密码不正确。");
  if(encoder.matches(input.newPassword(),hash))throw bad("新密码不能与当前密码相同。");
  repo.jdbc().update("UPDATE users SET password_hash=:hash,credential_version=credential_version+1 WHERE id=:id",Map.of("hash",encoder.encode(input.newPassword()),"id",user.id()));
 }
 @Transactional public void bootstrap(String name,String display,String pass){
  name=username(name);
  repo.jdbc().queryForList("SELECT id FROM users ORDER BY id FOR UPDATE",Map.of());
  var existing=repo.jdbc().queryForList("SELECT role FROM users WHERE username=:name",Map.of("name",name),String.class);
  if(!existing.isEmpty()){if(!existing.getFirst().equals("ADMIN"))throw new IllegalStateException("Bootstrap name belongs to a non-admin account. No privilege or password was changed.");System.out.println("Administrator already exists; password preserved.");return;}
  if(repo.jdbc().queryForObject("SELECT COUNT(*) FROM users WHERE role='ADMIN' AND username IS NOT NULL",Map.of(),Long.class)>0)throw new IllegalStateException("First-admin bootstrap already completed. No additional administrator was created.");
  password(pass);display=text(display,1,100,"显示名称");
  repo.jdbc().update("INSERT INTO users(public_id,username,display_name,password_hash,role,is_demo) VALUES(:public,:name,:display,:hash,'ADMIN',FALSE)",Map.of("public","account-"+UUID.randomUUID(),"name",name,"display",display,"hash",encoder.encode(pass)));
  System.out.println("First administrator created. Credentials are available only in the private local configuration.");
 }
}
