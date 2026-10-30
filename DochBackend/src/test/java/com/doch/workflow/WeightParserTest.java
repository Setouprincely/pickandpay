package com.doch.workflow;

import com.doch.Catalog.service.WeightParser;
import com.doch.common.exceptions.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class WeightParserTest {

    @Test
    void parsesKilogramsAndGrams() {
        assertEquals(2.0, WeightParser.toKilograms("2"));
        assertEquals(2.5, WeightParser.toKilograms("2.5 kg"));
        assertEquals(2.5, WeightParser.toKilograms("2,5kg"));
        assertEquals(0.5, WeightParser.toKilograms("500g"));
        assertEquals(0.25, WeightParser.toKilograms("250 grams"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "heavy", "-3", "0", "2 lbs", "6000"})
    void rejectsInvalidWeights(String raw) {
        assertThrows(BusinessException.class, () -> WeightParser.toKilograms(raw));
    }
}