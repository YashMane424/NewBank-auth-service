package com.project.NewBank.Security.request.AccountsManipulation;

import java.math.BigDecimal;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Data
@RequiredArgsConstructor
@Getter
@Setter
public class WithdrawRequest {
    
    private String accountNumber;
    private BigDecimal amount;
    private String username;
    private String currency="INR";
}
