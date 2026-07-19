package com.papertrade.domain.enums;

import java.time.Duration;
import java.time.LocalDate;

/**
 * Time ranges for the stock price-history chart.
 *
 * Each range maps to a Twelve Data {@code time_series} interval and a cache TTL.
 *
 * Two query strategies:
 * - Intraday ranges (1D, 1W) request the most recent N bars via {@code outputsize}.
 *   A start_date would break on weekends/holidays (no intraday bars exist for a
 *   non-trading day), so outputsize is used to always return the latest session.
 * - Daily ranges (3M, 1Y, YTD) request everything from a {@code start_date}; daily
 *   bars are dense enough that a non-trading start date just returns the next bar.
 */
public enum HistoryRange {

    ONE_DAY("1D", "5min", 78, Duration.ofMinutes(5)),      // ~one 6.5h session of 5-min bars
    ONE_WEEK("1W", "30min", 130, Duration.ofMinutes(15)),  // ~one week of 30-min bars
    THREE_MONTHS("3M", "1day", 0, Duration.ofHours(1)),
    ONE_YEAR("1Y", "1day", 0, Duration.ofHours(1)),
    YTD("YTD", "1day", 0, Duration.ofHours(1));

    private final String code;
    private final String interval;
    private final int outputSize; // > 0 => use outputsize (most recent N bars); 0 => use start_date
    private final Duration cacheTtl;

    HistoryRange(String code, String interval, int outputSize, Duration cacheTtl) {
        this.code = code;
        this.interval = interval;
        this.outputSize = outputSize;
        this.cacheTtl = cacheTtl;
    }

    public String interval() {
        return interval;
    }

    public Duration cacheTtl() {
        return cacheTtl;
    }

    /**
     * True when this range should query by number of recent bars (intraday),
     * false when it should query from a start date (daily).
     */
    public boolean usesOutputSize() {
        return outputSize > 0;
    }

    public int outputSize() {
        return outputSize;
    }

    /**
     * The start date to request from the provider, computed relative to today.
     * Only meaningful for daily ranges (see {@link #usesOutputSize()}).
     */
    public LocalDate startDate(LocalDate today) {
        return switch (this) {
            case THREE_MONTHS -> today.minusMonths(3);
            case ONE_YEAR -> today.minusYears(1);
            case YTD -> today.withDayOfYear(1);
            default -> today; // unused for intraday ranges
        };
    }

    /**
     * Resolve a client-supplied range code (e.g. "3M") to a HistoryRange.
     * Throws IllegalArgumentException (-> HTTP 400) for unknown codes.
     */
    public static HistoryRange fromCode(String code) {
        for (HistoryRange range : values()) {
            if (range.code.equalsIgnoreCase(code)) {
                return range;
            }
        }
        throw new IllegalArgumentException(
                "Invalid range: " + code + ". Valid values: 1D, 1W, 3M, 1Y, YTD");
    }
}
