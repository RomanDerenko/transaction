package com.example.tran.service.impl;

import com.example.tran.dto.rest.CreateTransactionDto;
import com.example.tran.dto.rest.TransactionsStatisticsDto;
import com.example.tran.error.OldTransactionException;
import com.example.tran.service.TransactionService;
import org.assertj.core.api.Java6Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Date;

@RunWith(SpringRunner.class)
public class TransactionServiceImplTest {

    private static final int ROUNDING_SCALE = 2;
    private static final int MINUTE_MILLISECONDS = 1000 * 60;

    @TestConfiguration
    static class EmployeeServiceImplTestContextConfiguration {

        @Bean
        public TransactionService transactionService() {
            return new TransactionServiceImpl();
        }
    }

    @Before
    public void init() {
        transactionService.deleteTransactions();
    }

    @Autowired
    private TransactionService transactionService;

    @Test
    public void getEmptyStatistics() {
        BigDecimal expectedZeroResult = BigDecimal.ZERO.setScale(2);

        TransactionsStatisticsDto transactionsStatisticsDto = transactionService.getTransactionsStatistics();
        Java6Assertions.assertThat(transactionsStatisticsDto.getCount()).isEqualTo(0);
        Java6Assertions.assertThat(transactionsStatisticsDto.getAvg()).isEqualTo(expectedZeroResult);
        Java6Assertions.assertThat(transactionsStatisticsDto.getMin()).isEqualTo(expectedZeroResult);
        Java6Assertions.assertThat(transactionsStatisticsDto.getMax()).isEqualTo(expectedZeroResult);
        Java6Assertions.assertThat(transactionsStatisticsDto.getAvg()).isEqualTo(expectedZeroResult);
    }

    @Test
    public void testTransactionStatisticsExpired() throws Exception {

        Date currentDate = new Date();
        BigDecimal transactionAmount = new BigDecimal(5).setScale(ROUNDING_SCALE, RoundingMode.UNNECESSARY);
        createTransaction(transactionAmount, currentDate);

        Thread.sleep(MINUTE_MILLISECONDS);

        BigDecimal expectedZeroResult = BigDecimal.ZERO.setScale(2);
        TransactionsStatisticsDto transactionsStatisticsDto = transactionService.getTransactionsStatistics();
        Java6Assertions.assertThat(transactionsStatisticsDto.getCount()).isEqualTo(0);
        Java6Assertions.assertThat(transactionsStatisticsDto.getAvg()).isEqualTo(expectedZeroResult);
        Java6Assertions.assertThat(transactionsStatisticsDto.getMin()).isEqualTo(expectedZeroResult);
        Java6Assertions.assertThat(transactionsStatisticsDto.getMax()).isEqualTo(expectedZeroResult);
        Java6Assertions.assertThat(transactionsStatisticsDto.getAvg()).isEqualTo(expectedZeroResult);
    }

    @Test
    public void createOneTransactionTest() throws Exception {
        Date currentDate = new Date();
        BigDecimal transactionAmount = new BigDecimal(5).setScale(ROUNDING_SCALE, RoundingMode.UNNECESSARY);
        createTransaction(transactionAmount, currentDate);

        TransactionsStatisticsDto transactionsStatisticsDto = transactionService.getTransactionsStatistics();
        Java6Assertions.assertThat(transactionsStatisticsDto.getCount()).isEqualTo(1);
        Java6Assertions.assertThat(transactionsStatisticsDto.getAvg()).isEqualTo(transactionAmount);
        Java6Assertions.assertThat(transactionsStatisticsDto.getMin()).isEqualTo(transactionAmount);
        Java6Assertions.assertThat(transactionsStatisticsDto.getMax()).isEqualTo(transactionAmount);
        Java6Assertions.assertThat(transactionsStatisticsDto.getAvg()).isEqualTo(transactionAmount);
    }

    @Test
    public void createMultipleTransactionsAtSameTimeTest() throws Exception {
        Date currentDate = new Date();

        BigDecimal transactionAmount = new BigDecimal(5).setScale(ROUNDING_SCALE, RoundingMode.UNNECESSARY);

        int transactionsCount = 5000;

        for (int i = 0; i < transactionsCount; i++) {
            createTransaction(transactionAmount, currentDate);
        }

        TransactionsStatisticsDto transactionsStatisticsDto = transactionService.getTransactionsStatistics();
        Java6Assertions.assertThat(transactionsStatisticsDto.getCount()).isEqualTo(transactionsCount);
        Java6Assertions.assertThat(transactionsStatisticsDto.getAvg()).isEqualTo(transactionAmount);
        Java6Assertions.assertThat(transactionsStatisticsDto.getMin()).isEqualTo(transactionAmount);
        Java6Assertions.assertThat(transactionsStatisticsDto.getMax()).isEqualTo(transactionAmount);
        Java6Assertions.assertThat(transactionsStatisticsDto.getAvg()).isEqualTo(transactionAmount);
    }

    @Test(expected=OldTransactionException.class)
    public void expiredTransactionTest() throws Exception {
        Date currentDate = new Date();
        Date expirationDate = new Date(currentDate.getTime() - MINUTE_MILLISECONDS);

        BigDecimal transactionAmount = getBigDecimal((double) 5);
        createTransaction(transactionAmount, expirationDate);
    }

    @Test
    public void bigTransactionLoadTest() throws Exception {
        int[] transactionAmounts = new int[]{3, 4, 5};
        int transactionsCount = 5000;

        Date newDate;

        for (int i = 0; i < transactionAmounts.length; ++i) {

            BigDecimal transactionAmount = getBigDecimal((double) transactionAmounts[i]);

            for (int j = 0; j < transactionsCount; j++) {
                newDate = new Date();

                createTransaction(transactionAmount, newDate);
                Thread.sleep(1);
            }
        }

        Double expectedAverage = Arrays.stream(transactionAmounts).average().orElse(0);
        Double expectedMin = (double) Arrays.stream(transactionAmounts).min().orElse(0);
        Double expectedMax = (double) Arrays.stream(transactionAmounts).max().orElse(0);

        TransactionsStatisticsDto transactionsStatisticsDto = transactionService.getTransactionsStatistics();
        Java6Assertions.assertThat(transactionsStatisticsDto.getCount()).isEqualTo(transactionsCount * transactionAmounts.length);
        Java6Assertions.assertThat(transactionsStatisticsDto.getAvg()).isEqualTo(getBigDecimal(expectedAverage));
        Java6Assertions.assertThat(transactionsStatisticsDto.getMin()).isEqualTo(getBigDecimal(expectedMin));
        Java6Assertions.assertThat(transactionsStatisticsDto.getMax()).isEqualTo(getBigDecimal(expectedMax));
    }

    @Test
    public void deleteTransactions() {
        transactionService.deleteTransactions();
    }

    private void createTransaction(BigDecimal amount, Date date) throws Exception {
        BigDecimal noticeAmount = amount.setScale(ROUNDING_SCALE, RoundingMode.UNNECESSARY); // all values should have this scale

        CreateTransactionDto createTransactionDto = new CreateTransactionDto();
        createTransactionDto.setAmount(noticeAmount);
        createTransactionDto.setTimestamp(date);

        transactionService.createTransaction(createTransactionDto);
    }

    private BigDecimal getBigDecimal(Double i) {
        return new BigDecimal(i).setScale(ROUNDING_SCALE, RoundingMode.UNNECESSARY);
    }


}