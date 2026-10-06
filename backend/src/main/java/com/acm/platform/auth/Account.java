package com.acm.platform.auth;
import java.io.Serializable;
public record Account(long id,String username,String displayName,String role,String createdAt,@com.fasterxml.jackson.annotation.JsonIgnore long credentialVersion) implements Serializable {
 public Account(long id,String username,String displayName,String role,String createdAt){this(id,username,displayName,role,createdAt,0);}
 public boolean admin(){return "ADMIN".equals(role);}
}
