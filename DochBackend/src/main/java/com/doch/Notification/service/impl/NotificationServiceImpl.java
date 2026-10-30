package com.doch.Notification.service.impl;

import com.doch.Notification.dto.NotificationResponseDTO;
import com.doch.Notification.model.Notification;
import com.doch.Notification.model.NotificationType;
import com.doch.Notification.model.UserNotifications;
import com.doch.Notification.repository.NotificationRepository;
import com.doch.Notification.repository.UserNotificationsRepository;
import com.doch.Notification.service.NotificationService;
import com.doch.User.model.User;
import com.doch.User.repository.UserRepository;
import com.doch.common.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
// import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationsRepository userNotificationsRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional // Ensures both saves happen together or not at all
    public Notification sendNotification(User user, NotificationType type, String message) {
        // Step 1: Create and save the generic Notification object.
        Notification notification = Notification.builder()
                .type(type)
                .message(message)
                .sentAt(LocalDateTime.now())
                .build();
        Notification savedNotification = notificationRepository.save(notification);

        // Step 2: Create the link between the User and the new Notification.
        UserNotifications userNotificationLink = UserNotifications.builder()
                .user(user)
                .notification(savedNotification)
                .isRead(false)
                .build();
        userNotificationsRepository.save(userNotificationLink);

        System.out.println("Notification created and linked for user " + user.getEmail());

        return savedNotification;
    }

    @Override
    public List<NotificationResponseDTO> getUnreadNotificationsForUser(User user) {
        // Use the new repository method to fetch the data
        List<UserNotifications> unreadLinks = userNotificationsRepository.findByUserAndIsReadFalseOrderByNotification_SentAtDesc(user);

        // Map the list of entities to a list of DTOs
        return unreadLinks.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(String userNotificationId, User user) {
        UserNotifications userNotification = userNotificationsRepository.findById(userNotificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + userNotificationId));

        // SECURITY CHECK: Ensure the user is not trying to read someone else's notification
        if (!userNotification.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to access this notification.");
        }

        userNotification.setRead(true);
        userNotification.setClickedAt(LocalDateTime.now());
        userNotificationsRepository.save(userNotification);
    }

    // Helper method to convert the entity to a DTO
    private NotificationResponseDTO toDto(UserNotifications userNotification) {
        return NotificationResponseDTO.builder()
                .id(userNotification.getId())
                .message(userNotification.getNotification().getMessage())
                .type(userNotification.getNotification().getType())
                .sentAt(userNotification.getNotification().getSentAt())
                .isRead(userNotification.isRead())
                .build();
    }

    /**
     * Sends notification to all users with a specific role.
     * Used for admin alerts (new orders, system events, etc.)
     */
    @Transactional
    public void sendNotificationToRole(String roleName, NotificationType type, String message) {
        // Find all users with this role and send notification to each
        userRepository.findAll().stream()
                .filter(u -> u.hasRole(roleName))
                .filter(u -> !u.getIsDeleted()) // Only active users
                .forEach(admin -> sendNotification(admin, type, message));
    }
}
