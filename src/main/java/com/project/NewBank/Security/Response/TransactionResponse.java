package com.project.NewBank.Security.Response;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.project.NewBank.model.Transaction.TransactionStatus;
import com.project.NewBank.model.Transaction.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private String transactionId;
    private BigDecimal transactionAmount;
    private TransactionType transactionType;
    private BigDecimal balance;
    private String description;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String referenceNumber;
    private LocalDateTime transactionDate;
    private TransactionStatus status;
}
