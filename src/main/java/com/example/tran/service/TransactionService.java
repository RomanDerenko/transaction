package com.example.tran.service;

import com.example.tran.dto.rest.CreateTransactionDto;
import com.example.tran.dto.rest.TransactionsStatisticsDto;

public interface TransactionService {
    void createTransaction(CreateTransactionDto createTransactionDto) throws Exception;
    TransactionsStatisticsDto getTransactionsStatistics();
    void deleteTransactions();
}
