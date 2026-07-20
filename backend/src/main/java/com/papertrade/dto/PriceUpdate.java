package com.papertrade.dto;

import java.math.BigDecimal;

/**
 * A single live price tick pushed to clients over SSE.
 */
public record PriceUpdate(String symbol, BigDecimal price, long timestamp) {}
