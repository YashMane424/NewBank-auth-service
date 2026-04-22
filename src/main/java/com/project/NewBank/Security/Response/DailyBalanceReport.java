package com.project.NewBank.Security.Response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder   //for optional parameters and user.builder() to give only specified parameters
public class DailyBalanceReport {
    private LocalDate reportDate;
    private String username;
    private BigDecimal totalBalance;
    private int accountCount;
    private int transactionCount;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private List<AccountSummary> accounts;   //Multiple accounts of same user

    @Data
    @Builder
    public static class AccountSummary {
        private String accountNumber;
        private String accountType;
        private BigDecimal balance;
        private int transactionsToday;
    }
}