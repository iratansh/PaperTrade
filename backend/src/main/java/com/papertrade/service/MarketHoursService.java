package com.papertrade.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Determines whether the US equity market is currently open.
 *
 * Weekday + time-window check against the configured regular session
 * (holidays are not modeled - acceptable for a paper-trading app).
 */
@Service
public class MarketHoursService {

    private final LocalTime open;
    private final LocalTime close;
    private final ZoneId zone;
    private final boolean forceOpen;

    public MarketHoursService(
            @Value("${trading.market-hours.open:09:30}") String open,
            @Value("${trading.market-hours.close:16:00}") String close,
            @Value("${trading.market-hours.timezone:America/New_York}") String zone,
            @Value("${trading.force-open:false}") boolean forceOpen) {
        this.open = LocalTime.parse(open);
        this.close = LocalTime.parse(close);
        this.zone = ZoneId.of(zone);
        this.forceOpen = forceOpen;
    }

    public boolean isMarketOpen() {
        if (forceOpen) {
            return true; // override for local testing / benchmarking
        }
        ZonedDateTime now = ZonedDateTime.now(zone);
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime time = now.toLocalTime();
        return !time.isBefore(open) && time.isBefore(close);
    }
}
