package com.doch.Catalog.service.Interface;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface ProductImageService {
    String saveProductImage(byte[] imageData, String fileName, String contentType);

    com.doch.Catalog.model.ProductImage getProductImageById(String imageId);

    void deleteProductImage(String imageId);

    ResponseEntity<Resource> getImageUrl(String imageId);
}
