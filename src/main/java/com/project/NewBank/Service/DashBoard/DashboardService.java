package com.project.NewBank.Service.DashBoard;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.NewBank.Security.Response.AccountResponse;
import com.project.NewBank.Security.Response.DashboardResponse;
import com.project.NewBank.Security.Response.TransactionResponse;
import com.project.NewBank.model.Account;
import com.project.NewBank.model.Transaction;
import com.project.NewBank.repository.AccountRepository;
import com.project.NewBank.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // Bean Injection by name — matches the {@Bean(name = "dashboardExecutor")}
    @Qualifier("dashboardExecutor")
    private final Executor dashboardExecutor;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData(String username) {
        long start = System.currentTimeMillis();

        List<Account> accounts = accountRepository.findByUserUsername(username);

        // Fire both heavy queries in parallel
        CompletableFuture<List<AccountResponse>> accountsFuture =
            CompletableFuture.supplyAsync(
                () -> accounts.stream()
                              .map(this::toAccountResponse)
                              .collect(Collectors.toList()),
                dashboardExecutor
            );

        CompletableFuture<List<TransactionResponse>> transactionsFuture =
            CompletableFuture.supplyAsync(
                () -> {
                    List<Transaction> txns = transactionRepository
                        .findRecentByAccounts(accounts, PageRequest.of(0, 20));
                    return txns.stream()
                               .map(this::toTransactionResponse)
                               .collect(Collectors.toList());
                },
                dashboardExecutor
            );

        CompletableFuture.allOf(accountsFuture, transactionsFuture).join();// 1.allOff-> Wait for both to complete , 
                                                                            // 2.join() to block until both are done
        log.debug("[Dashboard] Parallel fetch complete for user={} in {}ms",
            username, System.currentTimeMillis() - start);

        return DashboardResponse.builder()
            .accounts(accountsFuture.join())
            .recentTransactions(transactionsFuture.join())
            .build();
    }



    private AccountResponse toAccountResponse(Account a) {
        return AccountResponse.builder()
            .id(a.getId())
            .accountNumber(a.getAccountNumber())
            .accountType(a.getAccountType())
            .balance(a.getBalance())
            .currency(a.getCurrency())
            .build();
    }

    private TransactionResponse toTransactionResponse(Transaction t) {
        return TransactionResponse.builder()
            .id(t.getId())
            .transactionId(t.getTransactionId())
            .transactionType(t.getType())
            .transactionAmount(t.getAmount())
            .status(t.getStatus())
            .description(t.getDescription())
            .fromAccountNumber(t.getFromAccount() != null
                ? t.getFromAccount().getAccountNumber() : null)
            .toAccountNumber(t.getToAccount() != null
                ? t.getToAccount().getAccountNumber() : null)
            .balanceBefore(t.getBalanceBefore())
            .balanceAfter(t.getBalanceAfter())
            .transactionDate(t.getTransactionDate())
            .referenceNumber(t.getReferenceNumber())
            .build();
    }
}