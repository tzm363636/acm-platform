package com.acm.platform.auth;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Persisted credential version rejects every old session, including concurrent old-password logins. */
public class SessionValidityFilter extends OncePerRequestFilter {
 private final AuthService auth;private final SecurityContextRepository contexts;private final ObjectMapper json;
 public SessionValidityFilter(AuthService auth,SecurityContextRepository contexts,ObjectMapper json){this.auth=auth;this.contexts=contexts;this.json=json;}
 protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  var authentication=SecurityContextHolder.getContext().getAuthentication();
  if(authentication!=null&&authentication.getPrincipal() instanceof Account old){
   try{
    var fresh=auth.account(old.id());if(old.credentialVersion()!=fresh.credentialVersion())throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,"登录凭据已更新。");
    request.setAttribute("acm.current-account",fresh);
    if(!old.equals(fresh)){var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(fresh,null,List.of(new SimpleGrantedAuthority("ROLE_"+fresh.role()))));SecurityContextHolder.setContext(context);contexts.saveContext(context,request,response);}
   }catch(ResponseStatusException e){var session=request.getSession(false);if(session!=null)session.invalidate();SecurityContextHolder.clearContext();reply(response,401,"会话已失效，请重新登录。","SESSION_EXPIRED");return;}
   catch(org.springframework.dao.DataAccessException|org.springframework.transaction.TransactionException e){reply(response,503,"账户服务暂不可用，请稍后重试。","ACCOUNT_UNAVAILABLE");return;}
  }
  chain.doFilter(request,response);
 }
 private void reply(HttpServletResponse response,int status,String message,String code)throws IOException{response.setStatus(status);response.setContentType("application/json;charset=UTF-8");json.writeValue(response.getWriter(),Map.of("message",message,"code",code));}
}
