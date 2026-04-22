package com.project.NewBank.config;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.project.NewBank.model.Account;
import com.project.NewBank.model.Enum.RoleName;
import com.project.NewBank.model.Role;
import com.project.NewBank.model.User;
import com.project.NewBank.repository.AccountRepository;
import com.project.NewBank.repository.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initializeDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder, AccountRepository accountRepository) {
        return args -> {
            // Check if test user already exists
            User existingUser = userRepository.findByUsername("testuser");
            if (existingUser == null) {
                try {
                    
                    User testUser = new User();
                    testUser.setUsername("testuser");
                    String encodedPassword = passwordEncoder.encode("password123");
                    testUser.setPassword(encodedPassword);
                    testUser.setFullname("Test User");
                    testUser.setEmail("test@example.com");
                    
                    Set<Role> roles = new HashSet<>();
                    Role userRole = new Role(RoleName.ROLE_USER);
                    roles.add(userRole);
                    testUser.setRoles(roles);
                    
                    User saved = userRepository.save(testUser);   //-------Save this to DB for testing

                    Account account = Account.builder()
                        .accountType("SAVINGS")
                        .accountNumber("ACC170470812345612A9F3C4")
                        .user(saved)
                        .currency("USD")
                        .build();
                    account.setBalance(BigDecimal.valueOf(1000));
                    Account savedAccount = accountRepository.save(account);

                    Account checkingAccount = Account.builder()
                        .accountType("CHECKING")
                        .accountNumber("ACC270470812345612B8E2D5")
                        .user(saved)
                        .currency("USD")
                        .build();
                    checkingAccount.setBalance(BigDecimal.valueOf(2500));
                    Account savedChecking = accountRepository.save(checkingAccount);

                    System.out.println("✅ Test user created: username=testuser, password=password123");
                    System.out.println("   User ID: " + saved.getId() + ", Roles: " + saved.getRoles().size());
                    System.out.println("   Password (BCrypt): " + encodedPassword);
                    System.out.println("-----------------------------------------------------");

                    System.out.println("✅ Test user accounts created: ACC170470812345612A9F3C4 with balance $1000");
                    System.out.println("✅ Test user accounts created: ACC270470812345612B8E2D5 with balance $2500");
                    
                    System.out.println("   Account ID: " + savedAccount.getId() + ", Type: " + savedAccount.getAccountType());
                    System.out.println("   Account ID: " + savedChecking.getId() + ", Type: " + savedChecking.getAccountType());
                    System.out.println("-----------------------------------------------------");
                } catch (Exception e) {
                    System.err.println("Error creating test user: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("Test user already exists: " + existingUser.getUsername());
            }
        };
    }
}
