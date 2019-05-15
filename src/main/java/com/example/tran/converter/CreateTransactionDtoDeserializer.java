package com.example.tran.converter;

import com.example.tran.dto.rest.CreateTransactionDto;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CreateTransactionDtoDeserializer extends StdDeserializer<CreateTransactionDto> {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    @Override
    public CreateTransactionDto deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {

        JsonNode node;

        try {
            node = jsonParser.getCodec().readTree(jsonParser);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid json", e);
        }

        JsonNode amountNode = node.get("amount");
        JsonNode timestampNode = node.get("timestamp");

        if (amountNode == null || timestampNode == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Some required fields in json are missed");
        }

        BigDecimal amount;

        try {
            amount = new BigDecimal(amountNode.asText());
        }
        catch(NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Failed to parse 'amount' field", e);
        }

        Date date;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
        try {
            date = simpleDateFormat.parse(timestampNode.asText());
        } catch (ParseException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Failed to parse 'timestamp' field", e);
        }

        return CreateTransactionDto.builder()
                .amount(amount)
                .timestamp(date)
                .build();
    }

    public CreateTransactionDtoDeserializer() {
        this(null);
    }

    public CreateTransactionDtoDeserializer(Class<?> vc) {
        super(vc);
    }
}