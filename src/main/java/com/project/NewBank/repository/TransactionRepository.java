package com.project.NewBank.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.NewBank.model.Account;
import com.project.NewBank.model.Transaction;


@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Object> {
    
   List<Transaction> findByFromAccountOrToAccountOrderByTransactionDateDesc(Account fromAccount, Account toAccount);

   List<Transaction> findByFromAccountOrToAccountAndTransactionDateBetweenOrderByTransactionDateDesc(
    Account fromAccount,
    Account toAccount,
    LocalDateTime from,
    LocalDateTime to
   );

   @Query("""
      SELECT t FROM Transaction t
      WHERE t.fromAccount IN :accounts OR t.toAccount IN :accounts
      ORDER BY t.transactionDate DESC
   """)
   List<Transaction> findRecentByAccounts(
      @Param("accounts") List<Account> accounts,   //accounts parameter is in the query
      Pageable pageable  //to limit the number of results returned
   );

}
