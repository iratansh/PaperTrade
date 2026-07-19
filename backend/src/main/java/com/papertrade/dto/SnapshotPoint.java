package com.papertrade.dto;

import java.math.BigDecimal;

/**
 * A single point on the portfolio-value chart.
 */
public record SnapshotPoint(String capturedAt, BigDecimal totalValue) {}
