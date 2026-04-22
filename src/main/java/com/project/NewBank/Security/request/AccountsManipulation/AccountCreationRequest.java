package com.project.NewBank.Security.request.AccountsManipulation;

import java.math.BigDecimal;

import com.project.NewBank.model.Enum.AccountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;


@Data
public class AccountCreationRequest {    

    @NotBlank(message="Account holder name is required")
    @Pattern(regexp="^[a-zA-Z\\s]+$", message="Account holder name must contain only letters and spaces")
    private String accountHolderName;
    
    @NotBlank(message="Account type is required")
    @Pattern(regexp="^(SAVINGS|CHECKING|BUSINESS)$", message="Account type must be SAVINGS, CHECKING, or BUSINESS")
    private String accountType;
    
    @NotBlank(message="Currency is required")
    @Pattern(regexp="^[A-Z]{3}$", message="Currency must be a valid 3-letter ISO code")
    private String currency;
    
    @NotBlank(message="Initial deposit is required")
    @DecimalMin(value="0.0", inclusive=false, message="Initial deposit must be greater than zero")
    private BigDecimal initialDeposit;

    @NotBlank(message="Account type enum is required")
    private AccountType accountTypeEnum;

}
