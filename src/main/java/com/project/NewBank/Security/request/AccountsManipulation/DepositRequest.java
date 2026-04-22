package com.project.NewBank.Security.request.AccountsManipulation;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class DepositRequest {
    private String accountNumber;
    private BigDecimal amount;
    private String description;
}