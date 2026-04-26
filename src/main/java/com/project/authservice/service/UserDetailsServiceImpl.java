package com.project.authservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.project.authservice.model.User;
import com.project.authservice.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepo;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        System.out.println("[DEBUG] UserDetailsServiceImpl: Loading user by username: " + userName);
        User user = userRepo.findByUsername(userName);
        if (user == null) {
            System.out.println("[DEBUG] UserDetailsServiceImpl: User not found: " + userName);
            throw new UsernameNotFoundException("User not found");
        }
        System.out.println("[DEBUG] UserDetailsServiceImpl: User found: " + user.getUsername() + ", roles: " + user.getRoles());
        return UserDetailsImpl.build(user);
    }
}

