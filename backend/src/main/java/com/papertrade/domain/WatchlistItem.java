package com.papertrade.domain;

import com.papertrade.domain.enums.AlertDirection;
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
 * A symbol a user is tracking, optionally with a price alert.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("watchlist")
public class WatchlistItem {

    @Id
    private UUID watchlistId;

    private UUID userId;
    private String symbol;

    private BigDecimal alertPrice;          // nullable: no alert set
    private AlertDirection alertDirection;  // ABOVE/BELOW relative to price when set
    @Builder.Default
    private boolean triggered = false;      // fires once, then stays triggered

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
