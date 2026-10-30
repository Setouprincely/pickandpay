package com.doch.common.audit;

import com.doch.User.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /** Records an operation performed by the currently authenticated user (or SYSTEM when none). */
    @Transactional
    public void record(String action, String entityType, String entityId, String details) {
        User actor = currentUser();
        record(actor, action, entityType, entityId, details);
    }

    @Transactional
    public void record(User actor, String action, String entityType, String entityId, String details) {
        AuditLog entry = AuditLog.builder()
                .actorId(actor != null ? actor.getId() : null)
                .actorEmail(actor != null ? actor.getEmail() : "SYSTEM")
                .actorRole(actor != null && actor.getUserRole() != null ? actor.getUserRole().getName() : "SYSTEM")
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details != null && details.length() > 2000 ? details.substring(0, 2000) : details)
                .occurredAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(entry);
        log.info("AUDIT {} {}:{} by {} - {}", action, entityType, entityId, entry.getActorEmail(), details);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> search(String entityType, String entityId, String actorId, int page, int size) {
        return auditLogRepository.search(blankToNull(entityType), blankToNull(entityId), blankToNull(actorId),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200)));
    }

    public static User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}