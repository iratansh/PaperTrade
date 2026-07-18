package com.papertrade.dto;

import com.papertrade.domain.enums.OrderSide;
import com.papertrade.domain.enums.OrderStatus;
import com.papertrade.domain.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private UUID orderId;
    private String symbol;
    private OrderType type;
    private OrderSide side;
    private BigDecimal quantity;
    private BigDecimal limitPrice;
    private OrderStatus status;
    private BigDecimal filledPrice;
    private BigDecimal filledQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime filledAt;
}
