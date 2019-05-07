package com.example.tran.dto.rest;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class CreateTransactionDto {
    private BigDecimal amount;
    private Date timestamp;
}
