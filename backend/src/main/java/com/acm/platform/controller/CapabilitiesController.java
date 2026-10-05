package com.acm.platform.controller;
import com.acm.platform.db.PlatformRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api")
public class CapabilitiesController {
 final ObjectProvider<PlatformRepository> repo;final Environment env;
 public CapabilitiesController(ObjectProvider<PlatformRepository> repo,Environment env){this.repo=repo;this.env=env;}
 @GetMapping("/capabilities") public Object capabilities(){return Map.of("database",repo.getIfAvailable()!=null,"login",false,"realJudge",false,"demoWrites",env.getProperty("acm.db.demo-enabled",Boolean.class,false)&&"development".equals(env.getProperty("acm.db.environment")),"timeZone","Asia/Shanghai");}
}
