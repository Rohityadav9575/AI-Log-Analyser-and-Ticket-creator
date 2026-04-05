package com.loganalyzer.auth.entity;

import com.loganalyzer.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "users")
public class User extends BaseEntity {

    private String name;
    private String email;
    private String password; // hashed password
    private Role role;
    private String oauthId; // For Google Auth

}
