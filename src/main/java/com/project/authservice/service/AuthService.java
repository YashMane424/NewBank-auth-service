package com.project.authservice.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import com.project.authservice.response.SignupResponse;
import com.project.authservice.request.LoginRequest;
import com.project.authservice.request.SignupRequest;
import com.project.authservice.response.LoginResponse;
import com.project.authservice.model.Enum.RoleName;
import com.project.authservice.model.Role;
import com.project.authservice.model.User;
import com.project.authservice.repository.UserRepository;

@Service
public class AuthService {


    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest loginRequest) {
        System.out.println("[DEBUG] AuthService: Attempting login for username: " + loginRequest.getUsername());
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());

        try {
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            Object principal = authentication.getPrincipal();
            if (!(principal instanceof UserDetails)) {
                System.out.println("[DEBUG] AuthService: Authentication did not return a valid UserDetails principal");
                throw new RuntimeException("Authentication did not return a valid UserDetails principal");
            }
            UserDetails userDetails = (UserDetails) principal;
            System.out.println("[DEBUG] AuthService: Authenticated user: " + userDetails.getUsername());
            String token = jwtService.generateToken(new HashMap<>(), userDetails);
            String refreshToken = jwtService.createRefreshToken(new HashMap<>(), userDetails);

            
            LoginResponse response = new LoginResponse();
            response.setUsername(userDetails.getUsername());
            response.setRoles(userDetails.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toList()));
            response.setToken(token);
            response.setRefreshToken(refreshToken);
            return response;
        } catch (RuntimeException e) {
            System.out.println("[DEBUG] AuthService: Authentication failed for username: " + loginRequest.getUsername() + ", reason: " + e.getMessage());
            throw e;
        }
    }

    public SignupResponse signUp(SignupRequest signupRequest) {
        // check existing username
        if (userRepository.findByUsername(signupRequest.getUsername()) != null) {
            throw new RuntimeException("Username already exists");
        }

        String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());
        User user = new User(signupRequest.getUsername(), encodedPassword,
                        signupRequest.getFullname(), signupRequest.getEmail());

        Set<Role> roles = new HashSet<>();
        if (signupRequest.getRole() == null || signupRequest.getRole().isEmpty()) {
            roles.add(new Role(RoleName.ROLE_USER));
        } else {
            for (String r : signupRequest.getRole()) {
                if ("ROLE_ADMIN".equalsIgnoreCase(r) || "admin".equalsIgnoreCase(r)) {
                    roles.add(new Role(RoleName.ROLE_ADMIN));
                } else {
                    roles.add(new Role(RoleName.ROLE_USER));
                }
            }
        }

        user.setRoles(roles);
        User saved = userRepository.save(user);

        SignupResponse response = new SignupResponse();
        response.setUsername(saved.getUsername());
        response.setEmail(saved.getEmail());
        response.setFullname(saved.getFullname());
        response.setRole(saved.getRoles().stream().
                        map(r -> r.getName().name()).collect(Collectors.toSet()));
        return response;
    }
}
