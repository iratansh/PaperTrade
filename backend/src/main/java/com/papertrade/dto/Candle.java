package com.papertrade.dto;

import java.math.BigDecimal;

/**
 * A single OHLC candle for a point in time.
 *
 * datetime is kept as the raw string from the provider ("2024-01-02" for daily
 * bars, "2024-01-02 09:30:00" for intraday) so the frontend can use it directly
 * as a chart label without timezone/format juggling on the backend.
 */
public record Candle(
        String datetime,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume
) {}
