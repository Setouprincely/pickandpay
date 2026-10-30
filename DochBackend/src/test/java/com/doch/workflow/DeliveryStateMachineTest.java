package com.doch.workflow;

import com.doch.Logistic.model.DeliveryStateMachine;
import com.doch.Logistic.model.DeliveryStatus;
import com.doch.common.exceptions.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.doch.Logistic.model.DeliveryStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class DeliveryStateMachineTest {

    @ParameterizedTest
    @CsvSource({
            "PENDING_PAYMENT,PENDING_PREPARATION", "PENDING_PAYMENT,PENDING_ASSIGNMENT", "PENDING_PAYMENT,CANCELLED",
            "PENDING_PREPARATION,PENDING_ASSIGNMENT", "PENDING_ASSIGNMENT,DRIVER_ASSIGNED",
            "DRIVER_ASSIGNED,PICKED_UP", "DRIVER_ASSIGNED,DRIVER_ASSIGNED", "DRIVER_ASSIGNED,PENDING_ASSIGNMENT",
            "PICKED_UP,IN_TRANSIT", "PICKED_UP,AT_HUB", "AT_HUB,DRIVER_ASSIGNED", "AT_HUB,RETURNED",
            "IN_TRANSIT,DELIVERED", "IN_TRANSIT,FAILED", "FAILED,IN_TRANSIT", "FAILED,AT_HUB", "FAILED,RETURNED"
    })
    void allowsTheDocumentedSequence(DeliveryStatus from, DeliveryStatus to) {
        assertTrue(DeliveryStateMachine.canTransition(from, to));
        assertDoesNotThrow(() -> DeliveryStateMachine.assertTransition(from, to));
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING_PAYMENT,DRIVER_ASSIGNED", "PENDING_PAYMENT,DELIVERED", "PENDING_PREPARATION,DRIVER_ASSIGNED",
            "DRIVER_ASSIGNED,DELIVERED", "DRIVER_ASSIGNED,IN_TRANSIT", "PICKED_UP,DELIVERED", "PICKED_UP,CANCELLED",
            "IN_TRANSIT,CANCELLED", "DELIVERED,IN_TRANSIT", "DELIVERED,FAILED", "CANCELLED,PENDING_ASSIGNMENT",
            "RETURNED,DELIVERED"
    })
    void rejectsSkippedOrBackwardSteps(DeliveryStatus from, DeliveryStatus to) {
        assertFalse(DeliveryStateMachine.canTransition(from, to));
        assertThrows(BusinessException.class, () -> DeliveryStateMachine.assertTransition(from, to));
    }

    @Test
    void terminalStatesHaveNoExit() {
        for (DeliveryStatus terminal : new DeliveryStatus[]{DELIVERED, RETURNED, CANCELLED}) {
            assertTrue(DeliveryStateMachine.isTerminal(terminal));
            assertTrue(DeliveryStateMachine.allowedNext(terminal).isEmpty());
        }
    }

    @Test
    void custodyStatesAreThoseWhereDochHoldsThePackage() {
        assertTrue(DeliveryStateMachine.isInDochCustody(PICKED_UP));
        assertTrue(DeliveryStateMachine.isInDochCustody(AT_HUB));
        assertTrue(DeliveryStateMachine.isInDochCustody(IN_TRANSIT));
        assertFalse(DeliveryStateMachine.isInDochCustody(DRIVER_ASSIGNED));
        assertFalse(DeliveryStateMachine.isInDochCustody(DELIVERED));
    }
}