package com.doch.Notification.repository;

import com.doch.Notification.model.UserNotifications;
import com.doch.User.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserNotificationsRepository extends JpaRepository<UserNotifications, String> {
    /**
     * Finds all unread notifications for a given user, ordered by the most recent first.
     * The method name is parsed by Spring Data JPA to create the query.
     */
    List<UserNotifications> findByUserAndIsReadFalseOrderByNotification_SentAtDesc(User user);

}