package com.project.NewBank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.project.NewBank.model.Enum.AccountStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Entity
@NoArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long id;

    
    @Column(name = "account_number", unique = true, nullable = false)
    @Setter
    private String accountNumber;
    
    @Column(name = "balance", nullable = false)
    private BigDecimal balance;
    
    @Column(name = "account_type", nullable = false)
    private String accountType;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Getter
    @Setter
    private User user;

    @Column(name = "currency", nullable = false)
    private String currency;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status= AccountStatus.ACTIVE;

    @Builder
    public Account(String accountNumber, String accountType, User user, String currency) {
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.user = user;
        this.currency = currency;
    }
    
    
}
