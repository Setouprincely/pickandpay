package com.doch.common.settings;

import com.doch.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

/**
 * Single-row table holding the platform's delivery pricing configuration.
 * Editable by Super Admin via GET/PUT /api/settings/pricing.
 */
@Setter
@Getter
@Entity
@Table(name = "pricing_settings")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingSettings extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    /** Minimum fee just for starting a delivery. */
    @Builder.Default
    private Double basePrice = 800.0;

    /** Price added for every kilometer. */
    @Builder.Default
    private Double pricePerKm = 250.0;

    /** The absolute minimum a customer can pay for a delivery. */
    @Builder.Default
    private Double minPrice = 500.0;

    /** Percentage surcharge applied during peak periods (e.g. 20.0 = +20%). */
    @Builder.Default
    private Double peakSurchargePercent = 0.0;

    /** Whether the peak surcharge is currently active. */
    @Builder.Default
    private Boolean peakSurchargeEnabled = false;

    // --- Vehicle decision matrix (DOCH fleet) ---

    /** Bicycles carry packages up to this total weight. */
    @Builder.Default
    @Column(name = "bicycle_max_weight_kg")
    private Double bicycleMaxWeightKg = 5.0;

    /** Bicycles carry packages whose largest dimension does not exceed this value. */
    @Builder.Default
    @Column(name = "bicycle_max_dimension_cm")
    private Double bicycleMaxDimensionCm = 60.0;

    /** Suzuki Carry / K-Truck carries up to this weight; heavier loads need a Toyota HiAce. */
    @Builder.Default
    @Column(name = "k_truck_max_weight_kg")
    private Double kTruckMaxWeightKg = 350.0;

    // --- Marketplace publication rules ---

    /** When true, new seller listings require administrator approval before they are published. */
    @Builder.Default
    @Column(name = "product_moderation_required")
    private Boolean productModerationRequired = true;
}
