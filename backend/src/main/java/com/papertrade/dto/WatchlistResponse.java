package com.papertrade.dto;

import com.papertrade.domain.enums.AlertDirection;

import java.math.BigDecimal;

public record WatchlistResponse(
        String symbol,
        BigDecimal alertPrice,
        AlertDirection alertDirection,
        boolean triggered
) {}
