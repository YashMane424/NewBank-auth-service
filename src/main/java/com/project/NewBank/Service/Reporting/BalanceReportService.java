package com.project.NewBank.Service.Reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.NewBank.Security.Response.DailyBalanceReport;
import com.project.NewBank.model.Account;
import com.project.NewBank.model.Transaction;
import com.project.NewBank.model.User;
import com.project.NewBank.repository.AccountRepository;
import com.project.NewBank.repository.TransactionRepository;
import com.project.NewBank.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
//import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceReportService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // Runs every day at 1:00 AM
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional(readOnly = true)
    public void generateDailyBalanceReports() {
        log.info("[SCHEDULER] Daily balance report started — {}", LocalDateTime.now());
        long startMs = System.currentTimeMillis();

        List<User> allUsers = userRepository.findAll();
        int reportCount = 0;

        for (User user : allUsers) {
            try {
                DailyBalanceReport report = buildReportForUser(user);
                publishReport(report);
                reportCount++;
            } catch (Exception e) {
                // One user failure shouldn't stop the whole batch
                log.error("[SCHEDULER] Failed to generate report for user={}: {}",
                    user.getUsername(), e.getMessage());
            }
        }

        long elapsed = System.currentTimeMillis() - startMs;
        log.info("[SCHEDULER] Daily report complete — {} users processed in {}ms", reportCount, elapsed);
    }


    private DailyBalanceReport buildReportForUser(User user) {
        LocalDateTime dayStart = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime dayEnd   = LocalDate.now().atStartOfDay();

        List<Account> accounts = accountRepository.findByUser(user);

        BigDecimal totalBalance    = BigDecimal.ZERO;
        BigDecimal totalDeposits   = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        int txCount = 0;

        List<DailyBalanceReport.AccountSummary> summaries = new java.util.ArrayList<>();

        for (Account account : accounts) {
            totalBalance = totalBalance.add(account.getBalance());

            List<Transaction> txns = transactionRepository
                .findByFromAccountOrToAccountAndTransactionDateBetweenOrderByTransactionDateDesc(
                    account, account, dayStart, dayEnd
                );

            txCount += txns.size();

            for (Transaction tx : txns) {
                if (tx.getType() == Transaction.TransactionType.DEPOSIT) {
                    totalDeposits = totalDeposits.add(tx.getAmount());
                } else if (tx.getType() == Transaction.TransactionType.WITHDRAWAL) {
                    totalWithdrawals = totalWithdrawals.add(tx.getAmount());
                }
            }

            summaries.add(DailyBalanceReport.AccountSummary.builder()
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .transactionsToday(txns.size())
                .build());
        }

        return DailyBalanceReport.builder()
            .reportDate(LocalDate.now().minusDays(1))
            .username(user.getUsername())
            .totalBalance(totalBalance)
            .accountCount(accounts.size())
            .transactionCount(txCount)
            .totalDeposits(totalDeposits)
            .totalWithdrawals(totalWithdrawals)
            .accounts(summaries)
            .build();
    }

    private void publishReport(DailyBalanceReport report) {
        log.info("[REPORT] user={} date={} accounts={} balance={} deposits={} withdrawals={} txns={}",
            report.getUsername(),
            report.getReportDate(),
            report.getAccountCount(),
            report.getTotalBalance(),
            report.getTotalDeposits(),
            report.getTotalWithdrawals(),
            report.getTransactionCount()
        );
    }
}