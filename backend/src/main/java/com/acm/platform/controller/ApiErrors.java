package com.acm.platform.controller;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"请求失败。":e.getReason()));}
 @ExceptionHandler(DataAccessException.class) public ResponseEntity<?> database(){return ResponseEntity.status(503).body(Map.of("message","数据库服务暂不可用，请稍后重试。代码与草稿不会被清除。"));}
 @ExceptionHandler({IllegalArgumentException.class,HttpMessageNotReadableException.class}) public ResponseEntity<?> invalid(){return ResponseEntity.badRequest().body(Map.of("message","请求格式或参数无效。"));}
}
