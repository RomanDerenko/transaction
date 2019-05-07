package com.example.tran.Model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
public class Transaction {
    private BigDecimal amount;
    private Date timestamp;
}