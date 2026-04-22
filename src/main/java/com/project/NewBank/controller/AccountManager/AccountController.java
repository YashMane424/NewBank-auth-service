package com.project.NewBank.controller.AccountManager;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.NewBank.Security.Response.AccountResponse;
import com.project.NewBank.Security.Response.DashboardResponse;
import com.project.NewBank.Security.Response.TransactionResponse;
import com.project.NewBank.Security.request.AccountsManipulation.AccountCreationRequest;
import com.project.NewBank.Security.request.AccountsManipulation.DepositRequest;
import com.project.NewBank.Security.request.AccountsManipulation.TransferRequest;
import com.project.NewBank.Security.request.AccountsManipulation.WithdrawRequest;
import com.project.NewBank.Service.Accounting.AccountService;
import com.project.NewBank.Service.DashBoard.DashboardService;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    AccountService accountService;

    @Autowired
    DashboardService dashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(Authentication authentication) {
        return ResponseEntity.ok(
            dashboardService.getDashboardData(authentication.getName())
        );
    }
    
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts(Authentication authentication) {
        return ResponseEntity.ok(
            accountService.getUserAccounts(authentication.getName())
        );
    }

    @PostMapping("/create")
    public ResponseEntity<AccountResponse> createAccount(
            @RequestBody AccountCreationRequest req,
            Authentication authentication) {
        return ResponseEntity.ok(
            accountService.createAccount(req, authentication.getName())
        );
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @RequestBody WithdrawRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
            accountService.withdraw(request, authentication.getName())
        );
    }


    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @RequestBody DepositRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
            accountService.deposit(request, authentication.getName())
        );
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @RequestBody TransferRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
            accountService.transfer(request, authentication.getName())
        );
    }


    @GetMapping("/transactions/{accountNumber}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByAccount(
            @PathVariable String accountNumber,
            Authentication authentication) {
        return ResponseEntity.ok(
            accountService.getTransactionsByAccount(accountNumber, authentication.getName())
        );
    }
}