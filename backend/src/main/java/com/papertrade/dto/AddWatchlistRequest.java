package com.papertrade.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddWatchlistRequest {

    @NotBlank(message = "Symbol is required")
    private String symbol;

    // Optional: set a price alert. Null = just watch, no alert.
    private BigDecimal alertPrice;
}
