package com.project.authservice.response;

import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupResponse {
    private String username;

    private String fullname;
    
    private String password;

    private String email;

    private Set<String> role;
}
