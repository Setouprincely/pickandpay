package com.doch.Notification.service;

import com.doch.Notification.dto.NotificationResponseDTO;
import com.doch.Notification.model.Notification;
import com.doch.Notification.model.NotificationType;
import com.doch.User.model.User;

import java.util.List;

public interface NotificationService {

    /**
     * Creates and saves a notification for a specific user.
     *
     * @param user The recipient of the notification.
     * @param type The category of the notification.
     * @param message The content of the notification.
     * @return The saved Notification entity.
     */
    Notification sendNotification(User user, NotificationType type, String message);


    /**
     * Retrieves all unread notifications for a specific user.
     * @param user The user whose notifications are to be fetched.
     * @return A list of DTOs representing the unread notifications.
     */
    List<NotificationResponseDTO> getUnreadNotificationsForUser(User user);

    /**
     * Marks a specific notification as read for a given user.
     * @param userNotificationId The ID of the UserNotifications link record.
     * @param user The user who owns the notification (for security).
     */
    void markAsRead(String userNotificationId, User user);

    void sendNotificationToRole(String roleName, NotificationType type, String message);
}