package com.example.tran.service.impl;

import com.example.tran.Model.Transaction;
import com.example.tran.dto.rest.CreateTransactionDto;
import com.example.tran.dto.rest.TransactionsStatisticsDto;
import com.example.tran.error.IncomingDateTransactionException;
import com.example.tran.error.OldTransactionException;
import com.example.tran.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

@Component
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private static final int ROUNDING_SCALE = 2;
    private static final int MINUTE_MILLISECONDS = 1000 * 60;

    private Transaction[] latestTransactions = new Transaction[MINUTE_MILLISECONDS];
    private Transaction maxAmount = null;
    private Transaction minAmount = null;

    private Date allignedAt = new Date();

    @Override
    public void createTransaction(CreateTransactionDto createTransactionDto) throws Exception {

        Date currentDate = new Date();

        int position = getPositionInTransactionsArray(createTransactionDto.getTimestamp(), currentDate);

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
                    .build();
        } else {
            latestTransactions[position]
                    .setAmount(latestTransactions[position].getAmount().add(createTransactionDto.getAmount()));
            latestTransactions[position].setCount(latestTransactions[position].getCount() + 1);
        }

        Date expirationDate = new Date(currentDate.getTime() - MINUTE_MILLISECONDS);

        // update max value if needed
        if (maxAmount == null || expirationDate.after(createTransactionDto.getTimestamp()) ||
                maxAmount.getAmount().compareTo(createTransactionDto.getAmount()) < 0) {

            maxAmount = Transaction.builder()
                    .amount(createTransactionDto.getAmount())
                    .timestamp(createTransactionDto.getTimestamp())
                    .count(1L)
                    .build();
        }

        // update max value if needed
        if (minAmount == null || expirationDate.after(createTransactionDto.getTimestamp()) ||
                minAmount.getAmount().compareTo(createTransactionDto.getAmount()) > 0) {

            minAmount = Transaction.builder()
                    .amount(createTransactionDto.getAmount())
                    .timestamp(createTransactionDto.getTimestamp())
                    .count(1L)
                    .build();
        }
    }

    @Override
    public TransactionsStatisticsDto getTransactionsStatistics() {

        Date currentDate = new Date();
        shiftTransactionsArray(currentDate);

        BigDecimal sum = BigDecimal.ZERO;
        Long count = 0L;

        for (int pos = 0; pos < MINUTE_MILLISECONDS; ++pos) {

            Transaction transaction = latestTransactions[pos];
            if (transaction != null) {
                sum = sum.add(transaction.getAmount());
                count += transaction.getCount();
            }
        }

        BigDecimal max = maxAmount == null ? BigDecimal.ZERO : maxAmount.getAmount();
        BigDecimal min = minAmount == null ? BigDecimal.ZERO : minAmount.getAmount();
        BigDecimal avg = count == 0L ? BigDecimal.ZERO :
                sum.divide(BigDecimal.valueOf(count), ROUNDING_SCALE, RoundingMode.HALF_UP);

        return TransactionsStatisticsDto.builder()
                .avg(avg.setScale(ROUNDING_SCALE, RoundingMode.HALF_UP))
                .max(max.setScale(ROUNDING_SCALE, RoundingMode.HALF_UP))
                .min(min.setScale(ROUNDING_SCALE, RoundingMode.HALF_UP))
                .sum(sum.setScale(ROUNDING_SCALE, RoundingMode.HALF_UP))
                .count(count)
                .build();
    }

    @Override
    public void deleteTransactions() {
        for (int pos = 0; pos < MINUTE_MILLISECONDS; ++pos) {
            latestTransactions[pos] = null;
        }

        minAmount = null;
        maxAmount = null;
    }

    private void shiftTransactionsArray(Date currentDate) {

        // finding first expired date and shift offset
        Date expirationDate = new Date(currentDate.getTime() - MINUTE_MILLISECONDS);

        //delete min/max records if expired
        if (minAmount != null && minAmount.getTimestamp().before(expirationDate)) {
            minAmount = null;
        }

        if (maxAmount != null && maxAmount.getTimestamp().before(expirationDate)) {
            maxAmount = null;
        }

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
                    Transaction transactionToMove = latestTransactions[pos + shiftLength];
                    if (transactionToMove != null) {
                        latestTransactions[pos] = latestTransactions[pos + shiftLength];
                        latestTransactions[pos + shiftLength] = null;
                    }
                } else {
                    latestTransactions[pos] = null;
                }
            }
        }

        allignedAt = currentDate;
    }

    private int getTransactionsRecordsCount() {
        int res = 0;

        for (int pos = 0; pos < MINUTE_MILLISECONDS; ++pos) {
            if (latestTransactions[pos] != null) {
                res ++;
            }
        }

        return  res;
    }

    private int getPositionInTransactionsArray(Date transactionDate, Date currentDate)
            throws Exception {

        Long currentDateTimestamp = currentDate.getTime();
        Long transactionTimestamp = transactionDate.getTime();

        // too old transaction
        if (currentDateTimestamp - transactionTimestamp >= MINUTE_MILLISECONDS) {
            throw new OldTransactionException(String.format("Expired transaction received. " +
                            "Transaction date: %s, Current Date: %s", transactionDate.toString(),
                    currentDate.toString()));
        }

        // upcoming transaction
        if (transactionTimestamp > currentDateTimestamp) {
            throw new IncomingDateTransactionException(String.format("Incoming Date transaction received. " +
                            "Transaction date: %s, Current Date: %s", transactionDate.toString(),
                    currentDate.toString()));
        }

        // realign an array if transaction is newer than last update time
        if (transactionDate.after(allignedAt)) {
            shiftTransactionsArray(currentDate);
        }

        int timestampDiff = (int) (allignedAt.getTime() - transactionTimestamp);
        return MINUTE_MILLISECONDS - timestampDiff - 1;
    }
}
