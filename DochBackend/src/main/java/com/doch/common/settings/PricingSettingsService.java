package com.doch.common.settings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingSettingsService {

    private final PricingSettingsRepository pricingSettingsRepository;
    private final com.doch.common.audit.AuditService auditService;

    /**
     * Returns the single settings row, creating it with safe defaults on first access.
     * Defaults match the previous hard-coded pricing behaviour, so nothing changes
     * until an admin edits the values.
     */
    @Transactional
    public PricingSettings getOrCreateSettings() {
        return pricingSettingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    log.info("SETTINGS: No pricing settings found. Seeding defaults.");
                    return pricingSettingsRepository.save(PricingSettings.builder().build());
                });
    }

    @Transactional
    public PricingSettingsDTO getSettings() {
        return PricingSettingsDTO.fromEntity(getOrCreateSettings());
    }

    @Transactional
    public PricingSettingsDTO updateSettings(PricingSettingsDTO request) {
        PricingSettings settings = getOrCreateSettings();

        if (request.getBasePrice() != null) settings.setBasePrice(request.getBasePrice());
        if (request.getPricePerKm() != null) settings.setPricePerKm(request.getPricePerKm());
        if (request.getMinPrice() != null) settings.setMinPrice(request.getMinPrice());
        if (request.getPeakSurchargePercent() != null) settings.setPeakSurchargePercent(request.getPeakSurchargePercent());
        if (request.getPeakSurchargeEnabled() != null) settings.setPeakSurchargeEnabled(request.getPeakSurchargeEnabled());
        if (request.getBicycleMaxWeightKg() != null) settings.setBicycleMaxWeightKg(request.getBicycleMaxWeightKg());
        if (request.getBicycleMaxDimensionCm() != null) settings.setBicycleMaxDimensionCm(request.getBicycleMaxDimensionCm());
        if (request.getKTruckMaxWeightKg() != null) settings.setKTruckMaxWeightKg(request.getKTruckMaxWeightKg());
        if (request.getProductModerationRequired() != null) settings.setProductModerationRequired(request.getProductModerationRequired());
        if (settings.getKTruckMaxWeightKg() <= settings.getBicycleMaxWeightKg()) {
            throw new com.doch.common.exceptions.BusinessException("The K-Truck weight limit must be greater than the bicycle weight limit.");
        }
        auditService.record("SETTINGS_UPDATED", "SETTINGS", settings.getId(), PricingSettingsDTO.fromEntity(settings).toString());

        pricingSettingsRepository.save(settings);
        log.info("SETTINGS: Pricing settings updated: base={}, perKm={}, min={}, peak={}% (enabled={})",
                settings.getBasePrice(), settings.getPricePerKm(), settings.getMinPrice(),
                settings.getPeakSurchargePercent(), settings.getPeakSurchargeEnabled());

        return PricingSettingsDTO.fromEntity(settings);
    }
}
