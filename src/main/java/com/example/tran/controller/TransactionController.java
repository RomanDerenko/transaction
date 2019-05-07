package com.example.tran.controller;

import com.example.tran.dto.rest.CreateTransactionDto;
import com.example.tran.dto.rest.TransactionsStatisticsDto;
import com.example.tran.service.TransactionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class TransactionController {

    private TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transactions")
    @ApiOperation(value = "Create new transaction")
    ResponseEntity<Void> createTransaction(@RequestBody CreateTransactionDto createTransactionDto) {

        transactionService.createTransaction(createTransactionDto);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/statistics")
    @ApiOperation(value = "Get transactions statistics")
    ResponseEntity<TransactionsStatisticsDto> getTransactionsStatistics() {

        TransactionsStatisticsDto transactionsStatisticsDto = transactionService.getTransactionsStatistics();

        return new ResponseEntity<>(transactionsStatisticsDto, HttpStatus.OK);
    }

    @DeleteMapping("/transactions")
    @ApiOperation(value = "Delete all transactions")
    ResponseEntity<Void> deleteTransactions() {

        transactionService.deleteTransactions();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
