package com.doch.Catalog.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must be at most 200 characters")
    private String productName;

    @NotBlank(message = "Weight is required (in kg, e.g. 2.5)")
    @Size(max = 30)
    private String weight;

    @Size(max = 64)
    private String SKU;

    private List<String> imagerIds;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    @Max(value = 100_000_000, message = "Price is too large")
    private Double price;

    @Size(max = 3)
    private String currency;

    @NotNull(message = "Stock quantity is required")
    @PositiveOrZero(message = "Stock cannot be negative")
    @Max(value = 1_000_000, message = "Stock is too large")
    private Integer stockQuantity;

    @NotBlank(message = "Category ID is required")
    private String categoryId;

    @Positive(message = "Length must be positive")
    private Double lengthCm;
    @Positive(message = "Width must be positive")
    private Double widthCm;
    @Positive(message = "Height must be positive")
    private Double heightCm;
}