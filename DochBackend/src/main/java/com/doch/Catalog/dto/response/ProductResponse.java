package com.doch.Catalog.dto.response;

import com.doch.Catalog.model.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private String id;
    private String name;
    private List<String> imagerIds;
    private List<String> images;
    private String description;
    private double price;
    private String currency;
    private int stockQuantity;
    private String weight;
    private Double weightKg;
    private Double lengthCm;
    private Double widthCm;
    private Double heightCm;
    private String SKU;
    private long quantitySold;
    private List<Category> categories;

    private String sellerId;
    private String sellerName;
    private String sellerCity;
    private Boolean sellerVerified;

    /** Seller-controlled listing switch (deactivated listings are hidden). */
    private Boolean active;
    /** PENDING_REVIEW, PUBLISHED or REJECTED. */
    private String publicationStatus;
    private String rejectionReason;
    /** True when customers can currently see and buy this product. */
    private Boolean visibleToCustomers;
    private Date createdAt;
}