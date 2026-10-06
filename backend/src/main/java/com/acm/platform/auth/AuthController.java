package com.acm.platform.auth;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController @Profile({"mysql-local","aiven"}) @RequestMapping("/api/auth")
public class AuthController {
 final AuthService service;final LoginThrottle throttle;final SecurityContextRepository contexts;final SessionAuthenticationStrategy sessions;final HttpSessionCsrfTokenRepository csrf;
 public AuthController(AuthService s,LoginThrottle t,SecurityContextRepository c,SessionAuthenticationStrategy strategy,HttpSessionCsrfTokenRepository csrf){service=s;throttle=t;contexts=c;sessions=strategy;this.csrf=csrf;}
 @GetMapping("/csrf") public Object token(CsrfToken token){return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
 @GetMapping("/me") public Object me(Authentication a){return a!=null&&a.getPrincipal() instanceof Account?Map.of("user",service.current(a)):Collections.singletonMap("user",null);}
 @PostMapping("/register") public Object register(@RequestBody AuthService.Register body){return Map.of("user",service.register(body));}
 @PostMapping("/login") public Object login(@RequestBody AuthService.Login body,HttpServletRequest request,HttpServletResponse response){
  String name=body.username()==null?"":body.username();throttle.check(request.getRemoteAddr(),name);Account user;
  try{user=service.authenticate(body);}catch(ResponseStatusException e){if(e.getStatusCode().value()==401)throttle.failure(request.getRemoteAddr(),name);throw e;}
  var authentication=UsernamePasswordAuthenticationToken.authenticated(user,null,List.of(new SimpleGrantedAuthority("ROLE_"+user.role())));
  sessions.onAuthentication(authentication,request,response);
  var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(authentication);SecurityContextHolder.setContext(context);contexts.saveContext(context,request,response);throttle.success(name);
  return Map.of("user",user);
 }
 @PostMapping("/logout") public Object logout(Authentication auth,HttpServletRequest request,HttpServletResponse response){
  csrf.saveToken(null,request,response);new SecurityContextLogoutHandler().logout(request,response,auth);return Map.of("message","已退出登录。");
 }
}
