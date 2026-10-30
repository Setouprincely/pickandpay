package com.doch.Catalog.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import com.doch.Order.model.OrderItem;
import com.doch.User.model.Seller;
import com.doch.common.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "product")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class Product extends BaseEntity{
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @GeneratedValue
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;
    private String name;
    @Column(length = 2000)
    private String description;
    private double price;
    private String currency;
    private int stockQuantity;
    private boolean isActivity;
    /** Weight as entered by the seller (kept for display/backward compatibility). */
    private String weight;
    /** Normalised weight in kilograms, used for vehicle selection. */
    @Column(name = "weight_kg")
    private Double weightKg;
    @Column(name = "length_cm")
    private Double lengthCm;
    @Column(name = "width_cm")
    private Double widthCm;
    @Column(name = "height_cm")
    private Double heightCm;
    private String SKU;
    @Column(name = "quantity_sold", columnDefinition = "BIGINT DEFAULT 0")
    private long quantitySold;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", length = 30)
    private ProductPublicationStatus publicationStatus;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "reviewed_at")
    private java.time.LocalDateTime reviewedAt;

    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private Seller seller;


    // FIX: cascade = CascadeType.ALL removed. Category is SHARED reference data —
    // with cascade ALL, deleting a product cascades REMOVE onto its categories
    // (wiping them for every other product), and merging a detached product can
    // duplicate category rows. Products only ATTACH existing managed categories,
    // so no cascade is needed here.
    @ManyToMany
    @JoinTable(
            name = "product_category",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<Category> categories;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    public void addCategory(Category category){
        if (this.categories == null) {
            this.categories = new ArrayList<>();
        }
        this.categories.add(category);
    }

    /** A product is visible and purchasable only when published, active and sold by a verified seller. */
    public boolean isPubliclyVisible() {
        return publicationStatus == ProductPublicationStatus.PUBLISHED
                && isActivity
                && !Boolean.TRUE.equals(getIsDeleted())
                && seller != null
                && Boolean.TRUE.equals(seller.getIsVerified());
    }
}