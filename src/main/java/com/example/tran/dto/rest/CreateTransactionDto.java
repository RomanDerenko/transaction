package com.example.tran.dto.rest;

import com.example.tran.converter.CreateTransactionDtoDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@JsonDeserialize(using = CreateTransactionDtoDeserializer.class)
public class CreateTransactionDto {
    private BigDecimal amount;
    private Date timestamp;
}
