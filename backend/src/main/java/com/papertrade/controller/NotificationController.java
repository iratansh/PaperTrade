package com.papertrade.controller;

import com.papertrade.domain.Notification;
import com.papertrade.dto.NotificationResponse;
import com.papertrade.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Notifications (e.g. triggered price alerts) for the authenticated user.
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Flux<NotificationResponse> list(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return notificationService.list(userId).map(this::toResponse);
    }

    @GetMapping("/unread-count")
    public Mono<Map<String, Long>> unreadCount(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return notificationService.unreadCount(userId).map(count -> Map.of("count", count));
    }

    @PostMapping("/read")
    public Mono<Void> markAllRead(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return notificationService.markAllRead(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
            n.getNotificationId().toString(),
            n.getType(),
            n.getMessage(),
            n.isRead(),
            n.getCreatedAt().toString());
    }
}
