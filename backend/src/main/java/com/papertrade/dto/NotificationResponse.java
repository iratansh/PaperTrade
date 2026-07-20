package com.papertrade.dto;

public record NotificationResponse(
        String notificationId,
        String type,
        String message,
        boolean isRead,
        String createdAt
) {}
