package com.doch.common;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.doch.Catalog.dto.response.ProductResponse;
import com.doch.Catalog.model.Product;
import com.doch.Catalog.model.ProductImage;
import com.doch.Catalog.service.StoreMediaUsecase;
import com.doch.Order.dto.response.OrderItemResponse;
import com.doch.Order.dto.response.OrderResponse;
import com.doch.Order.model.Order;
import com.doch.Order.model.OrderItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class Mapper {

    private final StoreMediaUsecase storeMediaUsecase;

    /**
     * FIX: getPresignedUrl() throws a RuntimeException on any MinIO
     * error. Because the mapper is used by every product listing endpoint
     * (getAllProducts, seller products, category products, order items), a
     * single unreachable/misconfigured MinIO instance made ALL of those
     * endpoints return 500 — which on the seller dashboard looks exactly
     * like "adding products failed". A failed URL now degrades to null for
     * that image instead of killing the whole response.
     */
    private String safePresignedUrl(String objectKey) {
        if (objectKey == null) return null;
        try {
            return storeMediaUsecase.getPresignedUrl(objectKey);
        } catch (Exception e) {
            log.error("Could not generate presigned URL for object '{}': {}", objectKey, e.getMessage());
            return null;
        }
    }

    public OrderResponse toOrderResponse(Order order) {
        if (order == null) return null;

        return OrderResponse.builder()
                .orderId(order.getId())
                .reference(order.getReference())
                .status(order.getStatus() != null ? order.getStatus().name() : "PENDING")
                .contactPhone(order.getContactPhone())
                .paidAt(order.getPaidAt())
                .cancelledAt(order.getCancelledAt())
                .cancellationReason(order.getCancellationReason())

                // --- COST BREAKDOWN ---
                .productTotal(order.getProductTotal())
                .deliveryFee(order.getDeliveryFee())
                .totalAmount(order.getTotalAmount())

                // --- LOGISTICS INFO ---
                .transportType(order.getTransportType())

                .deliveryAddress(order.getDeliveryAddress())

                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName())
                .createdAt(order.getCreatedAt())
                .items(tOrderItemResponses(order.getItems()))
                .build();
    }

    public OrderItemResponse tOrderItemResponse(OrderItem item) {
        if (item == null) return null;

        OrderItemResponse response = new OrderItemResponse();
        response.setProductId(item.getProduct().getId());
        response.setProductName(item.getProduct().getName());
        if (item.getProduct().getSeller() != null) {
            response.setSellerId(item.getProduct().getSeller().getId());
            response.setSellerName(item.getProduct().getSeller().getName());
        }
        response.setWeightKg(item.getProduct().getWeightKg());
        response.setQuantity(item.getQuantity());
        response.setUnite_price(item.getUnite_priced());
        response.setTotal_price(item.getTotal_price());

        if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
            // Get the first image (or filter for is_primary if preferred)
            String imageKey = item.getProduct().getImages().get(0).getImageUrl();

            // Convert MinIO key to a viewable URL (fail-soft)
            response.setProductImageUrl(safePresignedUrl(imageKey));
        }

        return response;
    }

    public ProductResponse toProductResponse(Product product) {
        if (product == null) return null;

        ProductResponse response = new ProductResponse();
        response.setCurrency(product.getCurrency());
        response.setDescription(product.getDescription());
        response.setId(product.getId());
        response.setName(product.getName());
        response.setPrice(product.getPrice());
        response.setSKU(product.getSKU());
        response.setStockQuantity(product.getStockQuantity());
        response.setWeight(product.getWeight());
        response.setCategories(product.getCategories());
        response.setQuantitySold(product.getQuantitySold());
        response.setWeightKg(product.getWeightKg());
        response.setLengthCm(product.getLengthCm());
        response.setWidthCm(product.getWidthCm());
        response.setHeightCm(product.getHeightCm());
        response.setActive(product.isActivity());
        response.setPublicationStatus(product.getPublicationStatus() != null ? product.getPublicationStatus().name() : null);
        response.setRejectionReason(product.getRejectionReason());
        response.setCreatedAt(product.getCreatedAt());
        if (product.getSeller() != null) {
            response.setSellerId(product.getSeller().getId());
            response.setSellerName(product.getSeller().getName());
            response.setSellerCity(product.getSeller().getCity());
            response.setSellerVerified(Boolean.TRUE.equals(product.getSeller().getIsVerified()));
        }
        response.setVisibleToCustomers(product.isPubliclyVisible());

        // FIX: imagerIds previously called product.getImages().stream()
        // with no null guard (unlike the images field just below it),
        // which NPE'd on any product whose images collection was null.
        List<ProductImage> productImages = product.getImages() == null
                ? Collections.emptyList()
                : product.getImages();

        response.setImagerIds(
                productImages.stream()
                        .map(ProductImage::getId)
                        .toList()
        );
        response.setImages(
                productImages.stream()
                        .map(image -> safePresignedUrl(image.getImageUrl()))
                        .filter(Objects::nonNull)
                        .toList()
        );

        return response;
    }


    public List<ProductResponse> toProductResponseList(List<Product> products) {
        if (products == null) {
            return null;
        }

        return products.stream()
                .map(this::toProductResponse)
                .toList();
    }

    public List<OrderItemResponse> tOrderItemResponses(List<OrderItem> items) {
        if (items == null) return Collections.emptyList();
        return items.stream().map(this::tOrderItemResponse).toList();
    }

}