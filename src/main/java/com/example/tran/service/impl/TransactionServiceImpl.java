package com.example.tran.service.impl;

import com.example.tran.Model.Transaction;
import com.example.tran.dto.rest.CreateTransactionDto;
import com.example.tran.dto.rest.TransactionsStatisticsDto;
import com.example.tran.service.TransactionService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class TransactionServiceImpl implements TransactionService {

    private static final int ROUNDING_SCALE = 2;

    private List<Transaction> transactions = new CopyOnWriteArrayList<>();

    @Override
    public void createTransaction(CreateTransactionDto createTransactionDto) {
        transactions.add(Transaction.builder()
                .amount(createTransactionDto.getAmount())
                .timestamp(createTransactionDto.getTimestamp())
                .build());
    }

    @Override
    public TransactionsStatisticsDto getTransactionsStatistics() {

        BigDecimal sum = transactions.stream().map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal sumRounded = sum.setScale(ROUNDING_SCALE, RoundingMode.HALF_UP);

        BigDecimal max = transactions.stream().map(Transaction::getAmount)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO).setScale(ROUNDING_SCALE, RoundingMode.HALF_UP);

        BigDecimal min = transactions.stream().map(Transaction::getAmount)
                .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO).setScale(ROUNDING_SCALE, RoundingMode.HALF_UP);

        Long count = (long) transactions.size();

        BigDecimal avg = sum.divide(BigDecimal.valueOf(count), ROUNDING_SCALE, RoundingMode.HALF_UP);

        return TransactionsStatisticsDto.builder()
                .avg(avg)
                .max(max)
                .min(min)
                .sum(sumRounded)
                .build();
    }

    @Override
    public void deleteTransactions() {
        transactions.clear();
    }
}
