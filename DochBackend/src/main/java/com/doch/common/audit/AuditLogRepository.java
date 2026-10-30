package com.doch.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:entityType IS NULL OR a.entityType = :entityType)
          AND (:entityId IS NULL OR a.entityId = :entityId)
          AND (:actorId IS NULL OR a.actorId = :actorId)
        ORDER BY a.occurredAt DESC
        """)
    Page<AuditLog> search(@Param("entityType") String entityType,
                          @Param("entityId") String entityId,
                          @Param("actorId") String actorId,
                          Pageable pageable);
}