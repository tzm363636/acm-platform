package com.acm.platform.auth;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.authentication.session.*;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.http.HttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

@Configuration public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
 @Bean SecurityContextRepository contextRepository(){return new HttpSessionSecurityContextRepository();}
 @Bean HttpSessionCsrfTokenRepository csrfRepository(){return new HttpSessionCsrfTokenRepository();}
 @Bean SessionAuthenticationStrategy sessionStrategy(HttpSessionCsrfTokenRepository csrf){return new CompositeSessionAuthenticationStrategy(List.of(new ChangeSessionIdAuthenticationStrategy(),new CsrfAuthenticationStrategy(csrf)));}
 @Bean @ConditionalOnWebApplication(type=ConditionalOnWebApplication.Type.SERVLET)
 SecurityFilterChain security(HttpSecurity http,SecurityContextRepository context,HttpSessionCsrfTokenRepository csrf,ObjectMapper json,org.springframework.beans.factory.ObjectProvider<AuthService> auth)throws Exception {
  if(auth.getIfAvailable()!=null)http.addFilterAfter(new SessionValidityFilter(auth.getObject(),context,json),org.springframework.security.web.context.SecurityContextHolderFilter.class);
  http.csrf(x->x.csrfTokenRepository(csrf)).securityContext(x->x.securityContextRepository(context).requireExplicitSave(true))
   .authorizeHttpRequests(x->x.requestMatchers("/error","/api/health","/api/capabilities","/api/auth/csrf","/api/auth/login","/api/auth/register","/api/auth/me").permitAll()
    .requestMatchers("/api/admin/**").hasRole("ADMIN").requestMatchers("/api/account/**","/api/auth/logout").authenticated()
    .requestMatchers(HttpMethod.POST,"/api/oj/submissions").authenticated()
    .requestMatchers(HttpMethod.GET,"/api/articles/**","/api/article-options","/api/oj/**").permitAll().requestMatchers("/api/oj/demo/**").permitAll().anyRequest().denyAll())
   .formLogin(x->x.disable()).httpBasic(x->x.disable()).logout(x->x.disable()).requestCache(x->x.disable())
   .exceptionHandling(x->x.authenticationEntryPoint((request,response,e)->{response.setStatus(401);response.setContentType("application/json;charset=UTF-8");json.writeValue(response.getWriter(),Map.of("message","请先登录，或会话已过期。","code","UNAUTHENTICATED"));})
    .accessDeniedHandler((request,response,e)->{boolean csrfError=e instanceof org.springframework.security.web.csrf.CsrfException;var identity=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();boolean anonymous=identity==null||!identity.isAuthenticated()||identity instanceof org.springframework.security.authentication.AnonymousAuthenticationToken;boolean protectedPath=request.getRequestURI().startsWith("/api/account/")||request.getRequestURI().startsWith("/api/admin/")||request.getRequestURI().equals("/api/oj/submissions")||request.getRequestURI().equals("/api/auth/logout");response.setStatus(anonymous&&protectedPath?401:403);response.setContentType("application/json;charset=UTF-8");json.writeValue(response.getWriter(),Map.of("message",anonymous&&protectedPath?"请先登录。":csrfError?"安全令牌已失效，请重试。":"没有权限执行此操作。","code",csrfError?"CSRF_EXPIRED":"FORBIDDEN"));}));
  return http.build();
 }
}

