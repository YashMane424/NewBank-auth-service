package com.project.NewBank.Service.Accounting;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.NewBank.Security.Response.AccountResponse;
import com.project.NewBank.Security.Response.TransactionResponse;
import com.project.NewBank.Security.request.AccountsManipulation.AccountCreationRequest;
import com.project.NewBank.Security.request.AccountsManipulation.DepositRequest;
import com.project.NewBank.Security.request.AccountsManipulation.TransferRequest;
import com.project.NewBank.Security.request.AccountsManipulation.WithdrawRequest;
import com.project.NewBank.model.Account;
import com.project.NewBank.model.Enum.AccountStatus;
import com.project.NewBank.model.Transaction;
import com.project.NewBank.model.User;
import com.project.NewBank.repository.AccountRepository;
import com.project.NewBank.repository.TransactionRepository;
import com.project.NewBank.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<AccountResponse> getUserAccounts(String username) {
        log.info("Fetching all accounts for user: {}", username);
    
    // Find all accounts for this user
        List<Account> accounts = accountRepository.findByUserUsername(username);
    
    // Convert each entity to DTO
        return accounts.stream()
            .map(this::mapToAccountResponse)  // Method reference
            .collect(Collectors.toList());
    }

    public AccountResponse createAccount(AccountCreationRequest request, String username) {
        // to create new account for user
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Create and save the account
        Account account = Account.builder()
                .accountType(request.getAccountTypeEnum().name())
                .accountNumber(generateAccountNumber())
                .user(user)
                .currency(request.getCurrency())
                .build();
        account.setBalance(request.getInitialDeposit());
        Account savedAccount = accountRepository.save(account);
        log.info("Account created successfully for user: {}", username);

        return mapToAccountResponse(savedAccount);
    }

/////////////////////////////////////////////////
    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = "ACC" + System.currentTimeMillis() + UUID.randomUUID().toString();
        } while (accountRepository.existsByAccountNumber(accountNumber));
        
        return accountNumber;
    }
/////////////////////////////////////////////////
    private AccountResponse mapToAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .build();
    }
///////////////////////////////////////////////
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUserName(String username) {
        // to get account by user name
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        List<Account> accounts = accountRepository.findByUser(user);
        if (accounts == null || accounts.isEmpty()) {
            throw new RuntimeException("Account not found");
        }
        return accounts.stream().map(this::mapToAccountResponse).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNumber, String username) {
        // to get account by account number
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account == null || !account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Account not found");
        }
        return mapToAccountResponse(account);
    }














    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request,String username) {
        // to withdraw amount from account
        Account account = findAndValidateAccount(request.getAccountNumber(), username);
        
        validateWithdrawal(request.getAmount(), account.getBalance());
        
        BigDecimal balanceBefore = account.getBalance();

        Transaction transaction = createTransaction(
            account,                                
            null,                                   
            request.getAmount(),
            Transaction.TransactionType.WITHDRAWAL
        );   ///Initialize transaction as PENDING (transaction status will be updated later)
        transaction.setBalanceBefore(balanceBefore);
        transaction.setStatus(Transaction.TransactionStatus.PROCESSING);

        try{
            account.setBalance(account.getBalance().subtract(request.getAmount()));
            
            accountRepository.save(account);
            
            // 7. Complete transaction
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transaction.setBalanceAfter(account.getBalance());
            Transaction savedTransaction = transactionRepository.save(transaction);

            return mapToTransactionResponse(savedTransaction);
        }catch(Exception e){
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction); 
            
            throw new RuntimeException("Withdrawal failed: " + e.getMessage(), e);
        }

    }
    
    
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void validateWithdrawal( BigDecimal amount, BigDecimal balance) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                String.format("Insufficient funds. Balance:%s Requested:%s",balance,amount)
            );
        }
    }
















@Transactional
public TransactionResponse deposit(DepositRequest request, String username) {
    Account account = findAndValidateAccount(request.getAccountNumber(), username);

    if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("Deposit amount must be greater than zero");
    }

    BigDecimal balanceBefore = account.getBalance();

    Transaction transaction = createTransaction(
        null,                 //Always null for deposit maybe cash or something              
        account,                            
        request.getAmount(),
        Transaction.TransactionType.DEPOSIT
    );
    transaction.setBalanceBefore(balanceBefore);
    transaction.setStatus(Transaction.TransactionStatus.PROCESSING);

    try {

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setBalanceAfter(account.getBalance());
        transaction.setDescription(request.getDescription());
        Transaction saved = transactionRepository.save(transaction);

        return mapToTransactionResponse(saved);

    } catch (Exception e) {
        transaction.setStatus(Transaction.TransactionStatus.FAILED);
        transaction.setFailureReason(e.getMessage());
        transactionRepository.save(transaction);
        throw new RuntimeException("Deposit failed: " + e.getMessage(), e);
    }
}














@Transactional
public TransactionResponse transfer(TransferRequest request, String username) {

    Account fromAccount = findAndValidateAccount(request.getFromAccountNumber(), username);
    Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber());

    if (toAccount == null) {
        throw new RuntimeException("Destination account not found");
    }
    if (fromAccount.getAccountNumber().equals(toAccount.getAccountNumber())) {
        throw new IllegalArgumentException("Cannot transfer to the same account");
    }

    validateWithdrawal(request.getAmount(), fromAccount.getBalance());

    BigDecimal balanceBefore = fromAccount.getBalance();

    Transaction transaction = createTransaction(
        fromAccount, toAccount, request.getAmount(), Transaction.TransactionType.TRANSFER
    );
    transaction.setBalanceBefore(balanceBefore);
    transaction.setStatus(Transaction.TransactionStatus.PROCESSING);

    try {
        // Debit from source
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        // Credit to destination
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setBalanceAfter(fromAccount.getBalance());
        Transaction saved = transactionRepository.save(transaction);

        return mapToTransactionResponse(saved);
    } catch (Exception e) {
        transaction.setStatus(Transaction.TransactionStatus.FAILED);
        transaction.setFailureReason(e.getMessage());
        transactionRepository.save(transaction);
        throw new RuntimeException("Transfer failed: " + e.getMessage(), e);
    }
}















    private Account findAndValidateAccount(String accountNumber, String username) {
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if(account == null||!account.getUser().getUsername().equals(username)){
            throw new UsernameNotFoundException("Account not found: ");
        }
        
        // Verify account is active
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active: " + account.getStatus());
        }
        
        return account;
    }

    private Transaction createTransaction(
            Account fromAccount,
            Account toAccount,
            BigDecimal amount,
            Transaction.TransactionType type) {
        
        return Transaction.builder()
                .transactionId(generateTransactionId())
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .type(type)
                .status(Transaction.TransactionStatus.PENDING)
                .referenceNumber(UUID.randomUUID().toString())
                .transactionDate(LocalDateTime.now())
                .build();
    }

private TransactionResponse mapToTransactionResponse(Transaction transaction) {
    return TransactionResponse.builder()
            .id(transaction.getId())
            .transactionId(transaction.getTransactionId())
            .transactionType(transaction.getType())
            .transactionAmount(transaction.getAmount())
            .status(transaction.getStatus())
            .description(transaction.getDescription())
            .fromAccountNumber(transaction.getFromAccount() != null ?
                    transaction.getFromAccount().getAccountNumber() : null)
            .toAccountNumber(transaction.getToAccount() != null ?
                    transaction.getToAccount().getAccountNumber() : null)  
            .balanceBefore(transaction.getBalanceBefore())
            .balanceAfter(transaction.getBalanceAfter())
            .transactionDate(transaction.getTransactionDate())
            .referenceNumber(transaction.getReferenceNumber())
            .build();
}



    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByAccount(String accountNumber, String username) {
        Account account = findAndValidateAccount(accountNumber, username);

        List<Transaction> transactions = transactionRepository
            .findByFromAccountOrToAccountOrderByTransactionDateDesc(account, account);

        return transactions.stream()
            .map(this::mapToTransactionResponse)
            .collect(Collectors.toList());
    }

}