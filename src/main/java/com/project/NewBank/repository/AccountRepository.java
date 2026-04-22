package com.project.NewBank.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.NewBank.model.Account;
import com.project.NewBank.model.User;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findByAccountNumber(String accountNumber);

    Account findByUserId(Long userId);

    Account findByAccountType(String accountType);

    Account findByBalance(Double balance);
    
    Account findByAccountTypeAndUserId(String accountType, Long userId);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByUser(User user);

    List<Account> findByUserUsername(String username);

}
