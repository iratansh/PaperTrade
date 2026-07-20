package com.papertrade.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A user-facing notification (e.g. a triggered price alert).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("notifications")
public class Notification {

    @Id
    private UUID notificationId;

    private UUID userId;
    private String type;      // e.g. "PRICE_ALERT"
    private String message;

    @Builder.Default
    private boolean isRead = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
