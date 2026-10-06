package com.acm.platform.content;
import com.acm.platform.auth.*;
import org.springframework.security.core.Authentication;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController @Profile({"mysql-local","aiven"})
public class AccountController {
 final AuthService auth;final ArticleService articles;
 public AccountController(AuthService auth,ArticleService articles){this.auth=auth;this.articles=articles;}
 @GetMapping("/api/account/options") public Object options(){return articles.options();}
 @GetMapping("/api/account/articles") public Object list(Authentication a,@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="")String status,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size,@RequestParam(defaultValue="updated")String sort,@RequestParam(defaultValue="desc")String direction){return articles.list(auth.current(a),false,q,status,page,size,sort,direction);}
 @GetMapping("/api/account/articles/{id}") public Object article(Authentication a,@PathVariable long id){return articles.detail(auth.current(a),id);}
 @PostMapping("/api/account/articles") public Object create(Authentication a,@RequestBody ArticleInput in){return articles.create(auth.current(a),in);}
 @PutMapping("/api/account/articles/{id}") public Object save(Authentication a,@PathVariable long id,@RequestBody ArticleInput in){return articles.save(auth.current(a),id,in);}
 @PostMapping("/api/account/articles/{id}/{action:submit|withdraw}") public Object transition(Authentication a,@PathVariable long id,@PathVariable String action,@RequestBody ArticleService.Action input){return articles.transition(auth.current(a),id,action,input);}
 @GetMapping("/api/admin/articles") public Object adminArticles(Authentication a,@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="")String status,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size,@RequestParam(defaultValue="updated")String sort,@RequestParam(defaultValue="desc")String direction){return articles.list(auth.current(a),true,q,status,page,size,sort,direction);}
 @PostMapping("/api/admin/articles/{id}/{action:approve|reject|publish|archive}") public Object review(Authentication a,@PathVariable long id,@PathVariable String action,@RequestBody ArticleService.Action input){return articles.transition(auth.current(a),id,action,input);}
 @PutMapping("/api/admin/articles/{id}/flags") public Object flags(Authentication a,@PathVariable long id,@RequestBody ArticleService.Flags input){return articles.flags(auth.current(a),id,input);}
 @GetMapping("/api/admin/users") public Object users(Authentication a,@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return articles.users(auth.current(a),q,page,size);}
 @GetMapping("/api/admin/statistics") public Object statistics(Authentication a){return articles.statistics(auth.current(a));}
 @PostMapping("/api/admin/{kind:categories|tags}") public Object taxonomy(Authentication a,@PathVariable String kind,@RequestBody ArticleService.Name name){return articles.taxonomy(auth.current(a),kind,null,name);}
 @PutMapping("/api/admin/{kind:categories|tags}/{id}") public Object taxonomyEdit(Authentication a,@PathVariable String kind,@PathVariable long id,@RequestBody ArticleService.Name name){return articles.taxonomy(auth.current(a),kind,id,name);}
}
