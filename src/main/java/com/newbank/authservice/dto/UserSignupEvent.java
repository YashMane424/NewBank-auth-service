package com.newbank.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSignupEvent {
    private Long userId;
    private String username;
    private String email;
    private String fullname;
    private LocalDateTime signupAt;
    private String eventId;
}
