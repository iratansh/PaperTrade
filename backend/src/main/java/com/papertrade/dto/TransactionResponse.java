package com.papertrade.dto;

import com.papertrade.domain.enums.TransactionType;

import java.math.BigDecimal;

/**
 * A transaction (executed fill) in the account's audit log.
 */
public record TransactionResponse(
        String transactionId,
        String symbol,
        TransactionType type,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal fees,
        BigDecimal totalValue,
        String timestamp
) {}
