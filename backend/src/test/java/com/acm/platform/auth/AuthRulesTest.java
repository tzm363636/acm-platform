package com.acm.platform.auth;
import com.acm.platform.content.ArticleInput;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class AuthRulesTest {
 @Test void usernamesCanonicalAndAscii(){assertEquals("alice_123",AuthService.username("Alice_123"));assertThrows(RuntimeException.class,()->AuthService.username("ADMIN<script>"));}
 @Test void passwordByteLimit(){AuthService.password("long-test-password");assertThrows(RuntimeException.class,()->AuthService.password("短".repeat(25)));assertThrows(RuntimeException.class,()->AuthService.password("short"));}
 @Test void failuresLimitedEvenWhenAccountDoesNotExist(){var t=new LoginThrottle();for(int i=0;i<5;i++)t.failure("127.0.0.1","unknown");assertThrows(RuntimeException.class,()->t.check("127.0.0.1","unknown"));t.success("unknown");assertDoesNotThrow(()->t.check("127.0.0.1","unknown"));}
 @Test void perIpLimitCannotBeBypassedWithManyAccountNames(){var t=new LoginThrottle();for(int i=0;i<20;i++)t.failure("127.0.0.1","unknown"+i);assertThrows(RuntimeException.class,()->t.check("127.0.0.1","new-name"));}
 @Test void plainTextRejectsHtmlAndCodePreservesIt(){var body=new ArticleInput("中文文章","摘要",1L,List.of(),List.of(new ArticleInput.Section("章节",2,List.of("内容"),List.of(),"<script>\n原始代码\n</script>")),0);assertDoesNotThrow(body::validate);var injected=new ArticleInput("<script>alert(1)</script>","摘要",1L,List.of(),body.sections(),0);assertThrows(RuntimeException.class,injected::validate);}
}
