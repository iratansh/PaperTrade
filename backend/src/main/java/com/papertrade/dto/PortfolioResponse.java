package com.papertrade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {

    private BigDecimal cashBalance;
    private BigDecimal totalPositionValue;
    private BigDecimal totalPortfolioValue; // cash + positions
    private BigDecimal totalGainLoss; // unrealized P&L
    private BigDecimal totalGainLossPercentage;
    private List<PositionResponse> positions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionResponse {
        private String symbol;
        private BigDecimal quantity;
        private BigDecimal averageCost;
        private BigDecimal currentPrice;
        private BigDecimal currentValue;
        private BigDecimal unrealizedPnL;
        private BigDecimal unrealizedPnLPercentage;
    }
}
