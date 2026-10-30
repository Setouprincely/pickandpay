package com.doch.common.audit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@Tag(name = "11. Audit Trail", description = "Read-only audit trail of business and security operations.")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditService auditService;

    @Operation(summary = "Admin: Search the audit trail",
            description = "Filter by entityType (ORDER, PAYMENT, DELIVERY, PRODUCT, SELLER, USER, VEHICLE, SETTINGS), entityId and actorId.")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('MANAGE_SYSTEM', 'MANAGE_MARKET', 'MANAGE_XENDER')")
    public ResponseEntity<Page<AuditLog>> search(@RequestParam(required = false) String entityType,
                                                 @RequestParam(required = false) String entityId,
                                                 @RequestParam(required = false) String actorId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditService.search(entityType, entityId, actorId, page, size));
    }
}