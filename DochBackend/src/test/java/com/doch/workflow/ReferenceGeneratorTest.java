package com.doch.workflow;

import com.doch.common.ReferenceGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceGeneratorTest {

    @Test
    void formatsAreStableAndUnambiguous() {
        assertTrue(ReferenceGenerator.orderReference().matches("DO-[2-9A-HJKMNP-Z]{8}"));
        assertTrue(ReferenceGenerator.trackingNumber().matches("DCH-[2-9A-HJKMNP-Z]{10}"));
        assertTrue(ReferenceGenerator.numericOtp(6).matches("\\d{6}"));
    }

    @Test
    void referencesDoNotCollideInPractice() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 20_000; i++) {
            assertTrue(seen.add(ReferenceGenerator.trackingNumber()));
        }
    }
}