package com.example.tran.service.impl;

import com.example.tran.Model.Transaction;
import com.example.tran.dto.rest.CreateTransactionDto;
import com.example.tran.dto.rest.TransactionsStatisticsDto;
import com.example.tran.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private static final int ROUNDING_SCALE = 2;
    private static final int MINUTE_MILLISECONDS = 1000 * 60;

    private Transaction[] latestTransactions = new Transaction[MINUTE_MILLISECONDS];

    private Date allignedAt = new Date();

    @Override
    public synchronized void createTransaction(CreateTransactionDto createTransactionDto) {

        Date currentDate = new Date();

        int position = getPositionInTransactionsArray(createTransactionDto.getTimestamp(), currentDate);

        // debug
        if (position < 0 || position >= MINUTE_MILLISECONDS) {
            log.info(String.format("Error Position: %d", position));
        }

        Transaction transactionToUpdate = latestTransactions[position];

        // create/update transaction
        if (transactionToUpdate == null) {
            latestTransactions[position] = Transaction.builder()
                    .amount(createTransactionDto.getAmount())
                    .timestamp(createTransactionDto.getTimestamp())
                    .count(1L)
                    .max(createTransactionDto.getAmount())
                    .min(createTransactionDto.getAmount())
                    .build();
        } else {

            // update max value if needed
            if (latestTransactions[position].getMax().compareTo(createTransactionDto.getAmount()) < 0) {
                latestTransactions[position].setMax(createTransactionDto.getAmount());
            }

            // update min value if needed
            if (latestTransactions[position].getMin().compareTo(createTransactionDto.getAmount()) > 0) {
                latestTransactions[position].setMin(createTransactionDto.getAmount());
            }

            latestTransactions[position]
                    .setAmount(latestTransactions[position].getAmount().add(createTransactionDto.getAmount()));
            latestTransactions[position].setCount(latestTransactions[position].getCount() + 1);
        }
    }

    @Override
    public synchronized TransactionsStatisticsDto getTransactionsStatistics() {

        Date currentDate = new Date();
        shiftTransactionsArray(currentDate);

        BigDecimal sum = BigDecimal.ZERO;
        Long count = 0L;

        BigDecimal max = null;
        BigDecimal min = null;

        for (int pos = 0; pos < MINUTE_MILLISECONDS; ++pos) {

            Transaction transaction = latestTransactions[pos];
            if (transaction != null) {
                sum = sum.add(transaction.getAmount());
                count += transaction.getCount();

                if (max == null || max.compareTo(transaction.getMax()) < 0) {
                    max = transaction.getMax();
                }

                if (min == null || min.compareTo(transaction.getMin()) > 0) {
                    min = transaction.getMin();
                }
            }
        }

        BigDecimal avg = count == 0L ? BigDecimal.ZERO :
                sum.divide(BigDecimal.valueOf(count), ROUNDING_SCALE, RoundingMode.HALF_UP);

        return TransactionsStatisticsDto.builder()
                .avg(avg.setScale(ROUNDING_SCALE, RoundingMode.HALF_UP))
                .max((max == null ? BigDecimal.ZERO : max).setScale(ROUNDING_SCALE, RoundingMode.HALF_UP))
                .min((min == null ? BigDecimal.ZERO : min).setScale(ROUNDING_SCALE, RoundingMode.HALF_UP))
                .sum(sum.setScale(ROUNDING_SCALE, RoundingMode.HALF_UP))
                .count(count)
                .build();
    }

    @Override
    public synchronized void deleteTransactions() {
        for (int pos = 0; pos < MINUTE_MILLISECONDS; ++pos) {
            latestTransactions[pos] = null;
        }
    }

    private void shiftTransactionsArray(Date currentDate) {

        // finding first expired date and shift offset
        Date expirationDate = new Date(currentDate.getTime() - MINUTE_MILLISECONDS);

        // depending on last update time - shift an array
        int shiftLength;

        // passed 1 min and everything can be deleted
        if (expirationDate.after(allignedAt)) {
            shiftLength = MINUTE_MILLISECONDS - 1;
        }
        // calculate shift
        else {
            shiftLength = (int) (currentDate.getTime() - allignedAt.getTime());
        }


        // actual shifting elements
        if (shiftLength != 0) {
            for (int pos = 0; pos < MINUTE_MILLISECONDS; ++pos) {
                if (pos + shiftLength < MINUTE_MILLISECONDS) {
                    latestTransactions[pos] = latestTransactions[pos + shiftLength];
                    latestTransactions[pos + shiftLength] = null;
                } else {
                    if (latestTransactions[pos] != null) {
                        latestTransactions[pos] = null;
                    }
                }
            }
        }

        allignedAt = currentDate;
    }

    private int getTransactionsRecordsCount() {
        int res = 0;

        for (int pos = 0; pos < MINUTE_MILLISECONDS; ++pos) {
            if (latestTransactions[pos] != null) {
                res++;
            }
        }

        return res;
    }

    private Map<Integer, Transaction> getTransactionsRecords() {

        Map<Integer, Transaction> res = new HashMap<>();

        for (int pos = 0; pos < MINUTE_MILLISECONDS; ++pos) {
            if (latestTransactions[pos] != null) {
                res.put(pos, latestTransactions[pos]);
            }
        }

        return res;
    }

    private int getPositionInTransactionsArray(Date transactionDate, Date currentDate) {

        Long currentDateTimestamp = currentDate.getTime();
        Long transactionTimestamp = transactionDate.getTime();

        // too old transaction
        // TODO: there is an issue with NO_CONTENT exception handling (no exception body present)
        // it wil be fixed after migration spring boot to 3.1 (https://github.com/spring-projects/spring-framework/issues/12566)
        if (currentDateTimestamp - transactionTimestamp >= MINUTE_MILLISECONDS) {
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, String.format("Received expired transaction. " +
                    "Transaction date: %s", transactionDate.toString()));
        }

        // upcoming transaction
        if (transactionTimestamp > currentDateTimestamp) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("Incoming Date transaction received. " +
                            "Transaction date: %s, Current Date: %s)", transactionDate.toString(), currentDate.toString()));
        }

        // realign an array if transaction is newer than last update time
        if (transactionDate.after(allignedAt)) {
            shiftTransactionsArray(currentDate);
        }

        int timestampDiff = (int) (allignedAt.getTime() - transactionTimestamp);
        return MINUTE_MILLISECONDS - timestampDiff - 1;
    }
}
