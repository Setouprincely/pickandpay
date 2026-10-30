package com.doch.Logistic.service;

import com.doch.Logistic.model.Delivery;
import com.doch.Logistic.model.DeliveryEvent;
import com.doch.Logistic.model.DeliveryStatus;
import com.doch.Logistic.repository.DeliveryEventRepository;
import com.doch.User.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Writes the package tracking history. Every status change and scan creates one event. */
@Service
@RequiredArgsConstructor
public class TrackingEventService {

    public static final String CREATED = "CREATED";
    public static final String STATUS_CHANGE = "STATUS_CHANGE";
    public static final String ASSIGNED = "DRIVER_ASSIGNED";
    public static final String REASSIGNED = "DRIVER_REASSIGNED";
    public static final String UNASSIGNED = "DRIVER_UNASSIGNED";
    public static final String PICKUP_SCAN = "PICKUP_SCAN";
    public static final String HUB_SCAN = "HUB_SCAN";
    public static final String OTP_FAILED = "OTP_FAILED";
    public static final String OTP_LOCKED = "OTP_LOCKED";
    public static final String OTP_RESET = "OTP_RESET";
    public static final String RESCHEDULED = "RESCHEDULED";
    public static final String DELIVERED = "DELIVERED";
    public static final String EXCEPTION = "EXCEPTION";

    private final DeliveryEventRepository eventRepository;

    @Transactional
    public DeliveryEvent record(Delivery delivery, String eventType, DeliveryStatus from, DeliveryStatus to,
                                String publicMessage, String internalNote, User actor,
                                String hubName, String latitude, String longitude) {
        DeliveryEvent event = DeliveryEvent.builder()
                .delivery(delivery)
                .eventType(eventType)
                .fromStatus(from)
                .toStatus(to)
                .publicMessage(publicMessage)
                .internalNote(internalNote)
                .actorId(actor != null ? actor.getId() : null)
                .actorName(actor != null ? actor.getFirstName() + " " + actor.getLastName() : "System")
                .actorRole(actor != null && actor.getUserRole() != null ? actor.getUserRole().getName() : "SYSTEM")
                .hubName(hubName)
                .latitude(latitude)
                .longitude(longitude)
                .occurredAt(LocalDateTime.now())
                .build();
        return eventRepository.save(event);
    }

    @Transactional
    public DeliveryEvent record(Delivery delivery, String eventType, DeliveryStatus from, DeliveryStatus to,
                                String publicMessage, String internalNote, User actor) {
        return record(delivery, eventType, from, to, publicMessage, internalNote, actor, null, null, null);
    }

    /** Customer-facing wording for each status. */
    public static String describe(DeliveryStatus status) {
        if (status == null) return "Status updated";
        return switch (status) {
            case PENDING_PAYMENT -> "Awaiting payment";
            case PENDING_PREPARATION -> "Payment received - the seller is preparing your package";
            case PENDING_ASSIGNMENT -> "Package ready - waiting for a driver";
            case DRIVER_ASSIGNED -> "A driver has been assigned";
            case PICKED_UP -> "Package collected by the driver";
            case AT_HUB -> "Package processed at a DOCH hub";
            case IN_TRANSIT -> "Out for delivery";
            case DELIVERED -> "Delivered";
            case FAILED -> "Delivery attempt unsuccessful";
            case RETURNED -> "Returned to sender";
            case CANCELLED -> "Cancelled";
        };
    }
}