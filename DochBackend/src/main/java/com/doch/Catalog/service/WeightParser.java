package com.doch.Catalog.service;

import com.doch.common.exceptions.BusinessException;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts seller-entered weights ("2", "2.5 kg", "500g") into kilograms. */
public final class WeightParser {

    private static final Pattern WEIGHT = Pattern.compile("^\\s*([0-9]+(?:[.,][0-9]+)?)\\s*(kg|kgs|g|gr|grams?)?\\s*$");

    private WeightParser() {
    }

    public static double toKilograms(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("Weight is required (in kg, e.g. 2.5).");
        }
        Matcher m = WEIGHT.matcher(raw.toLowerCase(Locale.ROOT));
        if (!m.matches()) {
            throw new BusinessException("Invalid weight '" + raw + "'. Enter a number in kg (e.g. 2.5) or grams (e.g. 500g).");
        }
        double value = Double.parseDouble(m.group(1).replace(',', '.'));
        String unit = m.group(2);
        if (unit != null && unit.startsWith("g")) {
            value = value / 1000.0;
        }
        if (value <= 0 || value > 5000) {
            throw new BusinessException("Weight must be greater than 0 and at most 5000 kg.");
        }
        return value;
    }
}