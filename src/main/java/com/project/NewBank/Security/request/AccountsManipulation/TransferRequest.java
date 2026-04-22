package com.project.NewBank.Security.request.AccountsManipulation;

import java.math.BigDecimal;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class TransferRequest {
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String description;
}
