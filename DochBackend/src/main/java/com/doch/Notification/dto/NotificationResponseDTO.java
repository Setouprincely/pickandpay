package com.doch.Notification.dto;

import com.doch.Notification.model.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponseDTO {
    // The ID of the UserNotifications link, which is needed to mark it as read
    private String id;
    private String message;
    private NotificationType type;
    private LocalDateTime sentAt;
    private boolean isRead;
}