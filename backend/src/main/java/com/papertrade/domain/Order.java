package com.papertrade.domain;

import com.papertrade.domain.enums.OrderSide;
import com.papertrade.domain.enums.OrderStatus;
import com.papertrade.domain.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("orders")
public class Order {

    @Id
    private UUID orderId;

    private UUID accountId;

    private String symbol;

    private OrderType type; // MARKET, LIMIT

    private OrderSide side; // BUY, SELL

    private BigDecimal quantity;

    private BigDecimal limitPrice; // Only for LIMIT orders

    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    private BigDecimal filledPrice; // Actual execution price

    private BigDecimal filledQuantity; // For partial fills

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime filledAt;

    private String idempotencyKey; // Prevent duplicate orders

    /**
     * Calculate estimated cost for this order (for buy orders)
     */
    public BigDecimal getEstimatedCost() {
        if (side == OrderSide.BUY) {
            BigDecimal price = (type == OrderType.LIMIT) ? limitPrice : BigDecimal.ZERO;
            return price.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Mark order as filled
     */
    public void markFilled(BigDecimal executionPrice, BigDecimal executionQuantity) {
        this.status = OrderStatus.FILLED;
        this.filledPrice = executionPrice;
        this.filledQuantity = executionQuantity;
        this.filledAt = LocalDateTime.now();
    }

    /**
     * Mark order as cancelled
     */
    public void cancel() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be cancelled");
        }
        this.status = OrderStatus.CANCELLED;
    }

    /**
     * Check if order can be executed at given market price
     */
    public boolean canExecuteAtPrice(BigDecimal marketPrice) {
        if (type == OrderType.MARKET) {
            return true; // Market orders execute at any price
        }

        // Limit order execution logic
        if (side == OrderSide.BUY) {
            return marketPrice.compareTo(limitPrice) <= 0;
        } else {
            return marketPrice.compareTo(limitPrice) >= 0;
        }
    }
}
