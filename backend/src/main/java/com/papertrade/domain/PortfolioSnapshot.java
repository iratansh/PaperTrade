package com.papertrade.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A point-in-time snapshot of an account's total portfolio value.
 *
 * Written periodically by a scheduled job so the dashboard can chart real
 * portfolio growth over time - historical value can't be reconstructed from
 * current prices, so it must be recorded as it happens.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("portfolio_snapshots")
public class PortfolioSnapshot {

    @Id
    private UUID snapshotId;

    private UUID accountId;

    private BigDecimal totalValue;   // cash + positions
    private BigDecimal cashBalance;
    private BigDecimal positionValue;

    @Builder.Default
    private LocalDateTime capturedAt = LocalDateTime.now();
}
