package com.doch.Notification.controller;

import com.doch.Notification.dto.NotificationResponseDTO;
import com.doch.Notification.service.NotificationService;
import com.doch.User.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "5. Notifications", description = "Endpoints for managing user notifications.")
@SecurityRequirement(name = "bearerAuth") // All endpoints here require a user to be logged in
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "Get my unread notifications",
            description = "Retrieves a list of all notifications for the currently authenticated user that have not yet been marked as read."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of unread notifications.")
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid.")
    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getMyUnreadNotifications(Authentication authentication) {
        // Get the currently authenticated user from the security context
        User currentUser = (User) authentication.getPrincipal();
        List<NotificationResponseDTO> notifications = notificationService.getUnreadNotificationsForUser(currentUser);
        return ResponseEntity.ok(notifications);
    }

    @Operation(
            summary = "Mark a notification as read",
            description = "Marks a specific notification as read. A user can only mark their own notifications as read."
    )
    @ApiResponse(responseCode = "204", description = "Notification successfully marked as read.")
    @ApiResponse(responseCode = "401", description = "Unauthorized.")
    @ApiResponse(responseCode = "403", description = "Forbidden - User is trying to access another user's notification.")
    @ApiResponse(responseCode = "404", description = "Notification not found for the given ID.")
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markNotificationAsRead(@Parameter(description = "The ID of the user-notification link to be marked as read.") @PathVariable String id, Authentication authentication) {
        // Get the currently authenticated user for the security check
        User currentUser = (User) authentication.getPrincipal();
        notificationService.markAsRead(id, currentUser);
        // Return 204 No Content for a successful PATCH with no response body
        return ResponseEntity.noContent().build();
    }
}