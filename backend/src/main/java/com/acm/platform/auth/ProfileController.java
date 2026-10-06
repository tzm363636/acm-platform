package com.acm.platform.auth;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController @Profile({"mysql-local","aiven"}) @RequestMapping("/api/account")
public class ProfileController {
 final AuthService auth;final LoginThrottle throttle;
 public ProfileController(AuthService auth,LoginThrottle throttle){this.auth=auth;this.throttle=throttle;}
 @PutMapping("/profile") public Object profile(Authentication a,@RequestBody AuthService.Profile input){return Map.of("user",auth.updateProfile(auth.current(a),input));}
 @PostMapping("/password") public Object password(Authentication a,@RequestBody AuthService.PasswordChange input,HttpServletRequest request,HttpServletResponse response){
  var user=auth.current(a);String key="password:"+user.id();throttle.check(request.getRemoteAddr(),key);
  try{auth.changePassword(user,input);}catch(ResponseStatusException e){if(e.getStatusCode().value()==400)throttle.failure(request.getRemoteAddr(),key);throw e;}
  throttle.success(key);new SecurityContextLogoutHandler().logout(request,response,a);
  return Map.of("message","密码已更新，所有旧登录会话已失效，请重新登录。");
 }
}
