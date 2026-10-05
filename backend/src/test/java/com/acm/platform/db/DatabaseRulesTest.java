package com.acm.platform.db;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class DatabaseRulesTest {
 @Test void unexecutedScenariosKeepMetricsNull(){var problem=Map.<String,Object>of("timeLimit",1000,"memoryLimit",256);for(String scene:List.of("CE","SystemError","NetworkError")){assertNull(DemoService.time(problem,scene));assertNull(DemoService.memory(problem,scene));}assertEquals(1001,DemoService.time(problem,"TLE"));assertEquals(257.0,DemoService.memory(problem,"MLE"));}
 @Test void versionGate(){DatabaseLifecycle.checkVersion("8.4.8",8);assertThrows(IllegalStateException.class,()->DatabaseLifecycle.checkVersion("5.7.11",8));assertThrows(IllegalStateException.class,()->DatabaseLifecycle.checkVersion("8.0.15",8));assertThrows(IllegalStateException.class,()->DatabaseLifecycle.checkVersion("9.4.0",8));}
 @Test void targetFieldsCannotInjectUrl(){DatabaseConfig.validateTarget("service.example.com","25719","defaultdb");assertThrows(IllegalArgumentException.class,()->DatabaseConfig.validateTarget("host?sslMode=DISABLED","3306","db"));assertThrows(IllegalArgumentException.class,()->DatabaseConfig.validateTarget("host","3306","db;DROP"));}
 @Test void sortUsesWhitelist(){assertEquals("p.public_id DESC",PlatformRepository.order("id","desc",Map.of("id","p.public_id")));assertThrows(Exception.class,()->PlatformRepository.order("id;DROP","desc",Map.of("id","p.public_id")));assertThrows(Exception.class,()->PlatformRepository.order("id","desc;DROP",Map.of("id","p.public_id")));}
 @Test void likeSearchEscapesWildcards(){assertEquals("%a!%!_!!%",PlatformRepository.like("a%_!"));}
 @Test void paginationBoundaries(){assertEquals(100,Page.size(10000));assertEquals(1,Page.current(-1,0,8));assertEquals(2,Page.current(99,12,8));assertEquals(0,Page.of(List.of(),1,8,0).start());}
 @Test void sessionTokenIsHashed(){String value=DemoService.session("00000000-0000-4000-8000-000000000000");assertEquals(64,value.length());assertEquals(value,DemoService.session("00000000-0000-4000-8000-000000000000"));assertThrows(Exception.class,()->DemoService.session("invalid"));}
 @Test void clientCannotInjectVerdictOrTiming(){ObjectMapper mapper=new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);assertThrows(Exception.class,()->mapper.readValue("{\"code\":\"code\",\"scenario\":\"AC\",\"language\":\"cpp17\",\"verdict\":\"AC\",\"timeMs\":0}",DemoService.Request.class));}
 @Test void tlsPropertiesVerifyIdentity(){var properties=DatabaseConfig.connectionProperties(new MockEnvironment(),"VERIFY_IDENTITY");assertEquals("VERIFY_IDENTITY",properties.getProperty("sslMode"));assertEquals("false",properties.getProperty("allowPublicKeyRetrieval"));assertEquals("UTC",properties.getProperty("connectionTimeZone"));}
 @Test void aivenRejectsTlsDowngrade(){var env=new MockEnvironment().withProperty("acm.db.host","service.example.com").withProperty("acm.db.port","25719").withProperty("acm.db.name","defaultdb").withProperty("acm.db.ssl-mode","DISABLED");env.setActiveProfiles("aiven");assertThrows(IllegalArgumentException.class,()->new DatabaseConfig().dataSource(env));}
}
