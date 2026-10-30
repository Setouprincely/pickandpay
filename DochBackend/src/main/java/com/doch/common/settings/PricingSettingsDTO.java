package com.doch.common.settings;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY,
        getterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE,
        setterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
@NoArgsConstructor
@AllArgsConstructor
public class PricingSettingsDTO {
    @DecimalMin(value = "0.0", message = "Base price cannot be negative")
    private Double basePrice;
    @DecimalMin(value = "0.0", message = "Price per km cannot be negative")
    private Double pricePerKm;
    @DecimalMin(value = "0.0", message = "Minimum price cannot be negative")
    private Double minPrice;
    @DecimalMin(value = "0.0", message = "Peak surcharge cannot be negative")
    @DecimalMax(value = "300.0", message = "Peak surcharge cannot exceed 300%")
    private Double peakSurchargePercent;
    private Boolean peakSurchargeEnabled;

    // Vehicle decision matrix
    @DecimalMin(value = "0.1", message = "Bicycle weight limit must be positive")
    private Double bicycleMaxWeightKg;
    @DecimalMin(value = "1.0", message = "Bicycle dimension limit must be positive")
    private Double bicycleMaxDimensionCm;
    @DecimalMin(value = "1.0", message = "K-Truck weight limit must be positive")
    private Double kTruckMaxWeightKg;

    // Marketplace publication rules
    private Boolean productModerationRequired;

    public static PricingSettingsDTO fromEntity(PricingSettings s) {
        return PricingSettingsDTO.builder()
                .basePrice(s.getBasePrice())
                .pricePerKm(s.getPricePerKm())
                .minPrice(s.getMinPrice())
                .peakSurchargePercent(s.getPeakSurchargePercent())
                .peakSurchargeEnabled(s.getPeakSurchargeEnabled())
                .bicycleMaxWeightKg(s.getBicycleMaxWeightKg())
                .bicycleMaxDimensionCm(s.getBicycleMaxDimensionCm())
                .kTruckMaxWeightKg(s.getKTruckMaxWeightKg())
                .productModerationRequired(s.getProductModerationRequired())
                .build();
    }
}