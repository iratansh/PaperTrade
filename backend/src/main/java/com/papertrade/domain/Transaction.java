package com.papertrade.domain;

import com.papertrade.domain.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("transactions")
public class Transaction {

    @Id
    private UUID transactionId;

    private UUID orderId;

    private UUID accountId;

    private String symbol;

    private TransactionType type; // BUY, SELL

    private BigDecimal quantity;

    private BigDecimal price; // Execution price per share

    @Builder.Default
    private BigDecimal fees = BigDecimal.ZERO;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Calculate total transaction value (including fees)
     */
    public BigDecimal getTotalValue() {
        return price.multiply(quantity).add(fees);
    }

    /**
     * Calculate impact on account balance
     * Buy = negative (money out)
     * Sell = positive (money in)
     */
    public BigDecimal getBalanceImpact() {
        BigDecimal value = getTotalValue();
        return (type == TransactionType.BUY) ? value.negate() : value;
    }
}
