package com.papertrade.domain.enums;

public enum OrderStatus {
    PENDING,    // Order submitted, awaiting execution
    FILLED,     // Order successfully executed
    CANCELLED,  // Order cancelled by user
    REJECTED    // Order rejected (insufficient funds, invalid symbol, etc.)
}
