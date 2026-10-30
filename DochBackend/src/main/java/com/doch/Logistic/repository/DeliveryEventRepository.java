package com.doch.Logistic.repository;

import com.doch.Logistic.model.Delivery;
import com.doch.Logistic.model.DeliveryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryEventRepository extends JpaRepository<DeliveryEvent, String> {

    List<DeliveryEvent> findByDeliveryOrderByOccurredAtAsc(Delivery delivery);

    long countByDeliveryAndEventType(Delivery delivery, String eventType);
}