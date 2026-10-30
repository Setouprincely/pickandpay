package com.doch.Catalog.controller;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.doch.Catalog.service.Interface.ProductImageService;
import com.doch.common.exceptions.BusinessException;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/product/image")
@SecurityRequirement(name = "bearerAuth")
public class ProductImageController {

    private final ProductImageService productImageService;

    public ProductImageController(ProductImageService productImageService) {
        this.productImageService = productImageService;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CREATE_PRODUCT')")
    public String uploadProductImage(@RequestParam("image") MultipartFile image) throws IOException {
        com.doch.common.ImageUploadValidator.validate(image, "Product photo");
        return productImageService.saveProductImage(image.getBytes(),
                "products/" + java.util.UUID.randomUUID() + com.doch.common.ImageUploadValidator.extensionFor(image),
                image.getContentType());
    }

    @GetMapping("/download/{imageId}")
    public ResponseEntity<Resource> downloadProductImage(@PathVariable("imageId") String imageId) {

        return productImageService.getImageUrl(imageId);

    }
}
