package com.doch.Catalog.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.doch.Catalog.model.ProductImage;
import com.doch.Catalog.repository.ProductImageRepository;
import com.doch.Catalog.service.Interface.ProductImageService;
import com.doch.common.exceptions.BusinessException;

@Service
public class ProductImageServiceImpl implements ProductImageService{

    private final StoreMediaUsecase storeMediaUsecase;
    private final ProductImageRepository productImageRepository;

    public ProductImageServiceImpl(StoreMediaUsecase storeMediaUsecase,
            ProductImageRepository productImageRepository) {
        this.storeMediaUsecase = storeMediaUsecase;
        this.productImageRepository = productImageRepository;
    }

    @Override
    public String saveProductImage(byte[] imageData, String fileName, String contentType) {
        String imageUrl;
        try {
            imageUrl = storeMediaUsecase.storeInMinio(imageData, fileName, contentType);
        } catch (Exception e) {
            throw new BusinessException("Failed to store product image.");
        }

        ProductImage productImage = ProductImage.builder()
                .imageUrl(imageUrl)
                .names(fileName)
                .contentType(contentType)
                .build();

        productImageRepository.save(productImage);

        return productImage.getId();
    }

    @Override
    public ProductImage getProductImageById(String imageId) {
        return productImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Product Image not found with id: " + imageId));
    }

    @Override
    public void deleteProductImage(String imageId) {
        productImageRepository.deleteById(imageId);
    }

    @Override
    public ResponseEntity<Resource> getImageUrl(String imageId) {
        ProductImage productImage = getProductImageById(imageId);
        try {
            return ResponseEntity.ok(storeMediaUsecase.getFromMinio(productImage.getImageUrl()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
