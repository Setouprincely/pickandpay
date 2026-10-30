package com.doch.Logistic.service.impl;

import com.doch.Logistic.model.TransportType;
import com.doch.Logistic.service.PricingService;
import com.doch.common.settings.PricingSettings;
import com.doch.common.settings.PricingSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {

    private final PricingSettingsService pricingSettingsService;

    @Override
    public double calculateDeliveryPrice(double distanceKm, TransportType type) {
        double price;

        if (type == TransportType.FULL_TRUCK) {
            price = calculateBulkPrice(distanceKm);
        } else {
            price = calculateSingleParcelPrice(distanceKm);
        }

        // Apply admin-configured pricing settings (peak surcharge + minimum floor).
        // Defaults are seeded to match the tiered prices above, so behaviour is
        // unchanged until an admin edits /api/settings/pricing.
        PricingSettings settings = pricingSettingsService.getOrCreateSettings();

        if (Boolean.TRUE.equals(settings.getPeakSurchargeEnabled())
                && settings.getPeakSurchargePercent() != null
                && settings.getPeakSurchargePercent() > 0) {
            price = price * (1 + settings.getPeakSurchargePercent() / 100.0);
        }

        if (settings.getMinPrice() != null && price < settings.getMinPrice()) {
            price = settings.getMinPrice();
        }

        price = Math.round(price);
        log.info("PRICING: Type: {} | Distance: {} km | Final Price: {} XAF", type, distanceKm, price);
        return price;
    }

    private double calculateSingleParcelPrice(double distance) {
        if (distance <= 5.0) return 500.0;
        if (distance <= 10.0) return 700.0;
        if (distance <= 15.0) return 1000.0;
        if (distance <= 20.0) return 1200.0;
        return 1500.0; // Standard overage
    }

    private double calculateBulkPrice(double distance) {
        if (distance <= 5.0) return 7000.0;
        if (distance <= 10.0) return 8500.0;
        // Spreadsheet: "Above 10km (Within Bamenda)"
        return 10000.0;
    }
}
