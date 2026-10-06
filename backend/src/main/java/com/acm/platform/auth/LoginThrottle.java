package com.acm.platform.auth;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** In-memory, bounded limits for a single backend instance. Never retains plaintext credentials. */
@Component public class LoginThrottle {
 private record Attempt(int count,long expires){}
 private final Map<String,Attempt> attempts=new HashMap<>();private final Clock clock;
 public LoginThrottle(){this(Clock.systemUTC());} LoginThrottle(Clock clock){this.clock=clock;}
 private String key(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException();}}
 public synchronized void check(String ip,String username){purge();checkKey(key("ip:"+ip),20);checkKey(key("user:"+username.toLowerCase(Locale.ROOT)),5);}
 private void checkKey(String key,int limit){if(attempts.containsKey(key)&&attempts.get(key).count>=limit)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"登录尝试过于频繁，请 10 分钟后再试。");}
 public synchronized void failure(String ip,String username){purge();add(key("ip:"+ip));add(key("user:"+username.toLowerCase(Locale.ROOT)));}
 public synchronized void success(String username){attempts.remove(key("user:"+username.toLowerCase(Locale.ROOT)));}
 private void add(String key){var old=attempts.get(key);attempts.put(key,new Attempt(old==null?1:old.count+1,old==null?clock.millis()+600000:old.expires));}
 private void purge(){attempts.entrySet().removeIf(x->x.getValue().expires<=clock.millis());if(attempts.size()>10000)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"登录暂时繁忙，请稍后再试。");}
}
