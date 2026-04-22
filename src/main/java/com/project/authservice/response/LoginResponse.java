package com.project.NewBank.Security.Response;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private Long id;
    private String username;
    private List<String> roles;
    private String token;
    private String refreshToken;
}
