package com.papertrade.domain;

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
@Table("positions")
public class Position {

    @Id
    private UUID positionId;

    private UUID accountId;

    private String symbol;

    private BigDecimal quantity;

    private BigDecimal averageCost; // Average cost per share

    private BigDecimal currentPrice; // Cached from market data (updated periodically)

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    /**
     * Calculate current market value of this position
     */
    public BigDecimal getCurrentValue() {
        return currentPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate unrealized profit/loss
     */
    public BigDecimal getUnrealizedPnL() {
        BigDecimal costBasis = averageCost.multiply(quantity);
        BigDecimal currentValue = getCurrentValue();
        return currentValue.subtract(costBasis).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate unrealized P&L percentage
     */
    public BigDecimal getUnrealizedPnLPercentage() {
        BigDecimal costBasis = averageCost.multiply(quantity);
        if (costBasis.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getUnrealizedPnL()
            .divide(costBasis, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Update position after a buy transaction
     */
    public void addShares(BigDecimal sharesToAdd, BigDecimal pricePerShare) {
        BigDecimal currentCost = averageCost.multiply(quantity);
        BigDecimal additionalCost = pricePerShare.multiply(sharesToAdd);
        BigDecimal newQuantity = quantity.add(sharesToAdd);

        this.averageCost = currentCost.add(additionalCost)
            .divide(newQuantity, 2, RoundingMode.HALF_UP);
        this.quantity = newQuantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update position after a sell transaction
     */
    public void removeShares(BigDecimal sharesToRemove) {
        if (quantity.compareTo(sharesToRemove) < 0) {
            throw new IllegalStateException("Cannot sell more shares than owned");
        }
        this.quantity = quantity.subtract(sharesToRemove);
        this.updatedAt = LocalDateTime.now();
    }
}
