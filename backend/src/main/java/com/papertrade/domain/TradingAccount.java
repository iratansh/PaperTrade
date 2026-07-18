package com.papertrade.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("accounts")
public class TradingAccount {

    @Id
    private UUID accountId;

    private UUID userId;

    private BigDecimal balance;

    @Builder.Default
    private BigDecimal initialBalance = new BigDecimal("100000.00");

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // We use PESSIMISTIC locking at the repository layer
    // for balance updates to prevent race conditions during order placement

    /**
     * Check if account has sufficient funds for an order
     */
    public boolean canPlaceOrder(BigDecimal orderCost) {
        return balance.compareTo(orderCost) >= 0;
    }

    /**
     * Debit balance for a purchase
     */
    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
        this.balance = this.balance.subtract(amount);
    }

    /**
     * Credit balance for a sale
     */
    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
}
