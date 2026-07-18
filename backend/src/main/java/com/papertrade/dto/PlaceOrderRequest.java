package com.papertrade.dto;

import com.papertrade.domain.enums.OrderSide;
import com.papertrade.domain.enums.OrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {

    @NotNull(message = "Account ID is required")
    private UUID accountId;

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @NotNull(message = "Order type is required")
    private OrderType type;

    @NotNull(message = "Order side is required")
    private OrderSide side;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    // Only required for LIMIT orders
    private BigDecimal limitPrice;

    // Client-generated idempotency key to prevent duplicate submissions
    private String idempotencyKey;
}
