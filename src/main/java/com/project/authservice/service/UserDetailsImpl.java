package com.project.NewBank.Service.Security;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.project.NewBank.model.User;

public class UserDetailsImpl implements org.springframework.security.core.userdetails.UserDetails {

    private static final long serialVersionUID = 1L;
    private final String username;
    private final String password;
    private final String email;
    private final String fullname;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(String username, String email, String password, String fullname,
            Collection<? extends GrantedAuthority> authorities) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullname = fullname;
        this.authorities = authorities;
    }

    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().toString()))
                .collect(Collectors.toList());

        return new UserDetailsImpl(
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getFullname(),
                authorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
    
    public String getFullname() {
        return fullname;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
