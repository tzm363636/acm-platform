package com.acm.platform.controller;

import com.acm.platform.db.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Map;

@RestController @RequestMapping("/api") @Profile({"mysql-local","aiven"})
public class DataController {
 final PlatformRepository repo;final DemoService demo;
 public DataController(PlatformRepository repo,DemoService demo){this.repo=repo;this.demo=demo;}
 String kind(String mode){if(!mode.equals("demo")&&!mode.equals("real"))throw PlatformRepository.bad("无效数据模式。");return mode.equals("demo")?"DEMO":"REAL";}
 @GetMapping("/articles") public Object articles(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String category,@RequestParam(defaultValue="") String tag,@RequestParam(defaultValue="") String layout,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="8") int size,@RequestParam(defaultValue="id") String sort,@RequestParam(defaultValue="false") boolean includeOptions){return includeOptions?repo.articleFeed(q,category,tag,layout,page,size,sort):repo.articles(q,category,tag,layout,page,size,sort);}
 @GetMapping("/articles/{id}") public Object article(@PathVariable long id){return repo.article(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"文章不存在。"));}
 @GetMapping("/article-options") public Object articleOptions(){return repo.articleOptions();}
 @GetMapping("/oj/problems") public Object problems(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String difficulty,@RequestParam(defaultValue="") String tag,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="id") String sort,@RequestParam(defaultValue="asc") String direction,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="8") int size,@RequestParam(defaultValue="demo") String mode,@RequestHeader(value="X-Demo-Session",required=false) String token){return repo.problems(q,difficulty,tag,status,sort,direction,page,size,kind(mode),DemoService.session(token));}
 @GetMapping("/oj/problems/{id}") public Object problem(@PathVariable String id){return repo.problem(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"题目不存在。"));}
 @GetMapping("/oj/options") public Object options(){return repo.options();}
 @GetMapping("/oj/practice") public Object practice(@RequestParam(defaultValue="demo") String mode,@RequestHeader(value="X-Demo-Session",required=false) String token){return repo.practice(kind(mode),DemoService.session(token));}
 @GetMapping("/oj/submissions") public Object submissions(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String language,@RequestParam(defaultValue="") String verdict,@RequestParam(defaultValue="") String user,@RequestParam(defaultValue="all") String scope,@RequestParam(defaultValue="time") String sort,@RequestParam(defaultValue="desc") String direction,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="8") int size,@RequestParam(defaultValue="demo") String mode,@RequestHeader(value="X-Demo-Session",required=false) String token){return repo.submissions(q,language,verdict,user,scope,sort,direction,page,size,kind(mode),DemoService.session(token));}
 @GetMapping("/oj/submissions/{id}") public Object submission(@PathVariable String id,@RequestHeader(value="X-Demo-Session",required=false) String token){return repo.submission(id,DemoService.session(token)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"提交不存在。"));}
 @PostMapping("/oj/demo/problems/{id}/run") public Object run(@PathVariable String id,@RequestBody DemoService.Request request){return demo.run(id,request);}
 @PostMapping("/oj/demo/problems/{id}/submissions") public Object submit(@PathVariable String id,@RequestBody DemoService.Request request,@RequestHeader(value="X-Demo-Session",required=false) String token){return demo.submit(id,request,DemoService.session(token));}
 @PostMapping("/oj/demo/submissions/{id}/advance") public Object advance(@PathVariable String id,@RequestHeader(value="X-Demo-Session",required=false) String token){return demo.advance(id,DemoService.session(token));}
 @PostMapping("/oj/submissions") public Object realSubmit(){throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,"判题服务未接入，正式提交未开放；演示结果不会计入真实练习统计。");}
}
