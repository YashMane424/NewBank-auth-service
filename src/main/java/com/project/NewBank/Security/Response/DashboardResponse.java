package com.project.NewBank.Security.Response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private List<AccountResponse> accounts;
    private List<TransactionResponse> recentTransactions;
}
