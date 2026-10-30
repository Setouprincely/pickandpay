package com.doch.Catalog.controller;

import com.doch.Catalog.dto.request.ProductRequest;
import com.doch.Catalog.dto.response.ProductResponse;
import com.doch.Catalog.service.Interface.ProductService;
import com.doch.User.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Tag(name = "1. Catalogue", description = "Marketplace products: public catalogue, seller listings and admin moderation.")
public class ProductController {

    private final ProductService productService;
    private final Validator validator;

    private static User userOrNull(Authentication auth) {
        return auth != null && auth.getPrincipal() instanceof User u ? u : null;
    }

    // --- PUBLIC CATALOGUE ---

    @Operation(summary = "Public catalogue",
            description = "Published, active products of verified sellers. Optional filters: q (name/description), categoryId, sellerId.")
    @GetMapping
    public List<ProductResponse> getAllProducts(@RequestParam(required = false) String q,
                                                @RequestParam(required = false) String categoryId,
                                                @RequestParam(required = false) String sellerId) {
        return productService.getPublicCatalogue(q, categoryId, sellerId);
    }

    @Operation(summary = "Public catalogue: products of a category")
    @GetMapping("/category/{categoryId}")
    public List<ProductResponse> getCategoryProducts(@PathVariable String categoryId) {
        return productService.getPublicCatalogue(null, categoryId, null);
    }

    @Operation(summary = "Public catalogue: products of a seller shop")
    @GetMapping("/seller/{sellerId}")
    public List<ProductResponse> getSellerProducts(@PathVariable String sellerId) {
        return productService.getPublicCatalogue(null, null, sellerId);
    }

    @Operation(summary = "Product page", description = "Unpublished products are only visible to their seller and to admins.")
    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable String productId, Authentication auth) {
        return productService.getProduct(productId, userOrNull(auth));
    }

    // --- SELLER ---

    @Operation(summary = "Seller: All my products (any publication status)")
    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('CREATE_PRODUCT')")
    @SecurityRequirement(name = "bearerAuth")
    public List<ProductResponse> getMyProducts(Authentication auth) {
        return productService.getMyProducts((User) auth.getPrincipal());
    }

    @Operation(summary = "Seller: Create a product (JSON, images uploaded first via /addPicture)",
            description = "New listings go to PENDING_REVIEW when moderation is enabled (Admin > Settings).")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('CREATE_PRODUCT')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request, (User) auth.getPrincipal()));
    }

    @Operation(summary = "Seller: Create a product with its main photo (multipart)")
    @PostMapping(value = "/productWithImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CREATE_PRODUCT')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProductResponse> createProductWithImage(
            @Parameter(description = "Product photo (JPEG, PNG or WEBP, max 5 MB)", required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE))
            @RequestParam("image") MultipartFile image,
            @RequestParam("productName") String productName,
            @RequestParam("weight") String weight,
            @RequestParam(value = "SKU", required = false) String SKU,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("price") Double price,
            @RequestParam(value = "currency", required = false) String currency,
            @RequestParam("stockQuantity") Integer stockQuantity,
            @RequestParam("categoryId") String categoryId,
            @RequestParam(value = "lengthCm", required = false) Double lengthCm,
            @RequestParam(value = "widthCm", required = false) Double widthCm,
            @RequestParam(value = "heightCm", required = false) Double heightCm,
            Authentication auth) {
        ProductRequest request = new ProductRequest(productName, weight, SKU, null, description, price, currency,
                stockQuantity, categoryId, lengthCm, widthCm, heightCm);
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new com.doch.common.exceptions.BusinessException(violations.iterator().next().getMessage());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProductWithImage(image, request, (User) auth.getPrincipal()));
    }

    @Operation(summary = "Seller: Upload an extra product photo; returns its image ID")
    @PostMapping(value = "/addPicture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CREATE_PRODUCT')")
    @SecurityRequirement(name = "bearerAuth")
    public String addProductImage(@RequestParam("image") MultipartFile image, Authentication auth) {
        return productService.addProductImage(image, (User) auth.getPrincipal());
    }

    @Operation(summary = "Seller/Admin: Update a product", description = "Only the owning seller or catalogue admins. Rejected listings are resubmitted for review.")
    @PutMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    public ProductResponse updateProduct(@PathVariable String productId, @Valid @RequestBody ProductRequest request, Authentication auth) {
        return productService.updateProduct(request, productId, (User) auth.getPrincipal());
    }

    @Operation(summary = "Seller/Admin: Activate or deactivate a listing", description = "Body: { \"active\": true|false }. Deactivated listings disappear from the marketplace.")
    @PatchMapping("/{productId}/active")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    public ProductResponse setActive(@PathVariable String productId, @RequestBody Map<String, Boolean> body, Authentication auth) {
        Boolean active = body.get("active");
        if (active == null) throw new com.doch.common.exceptions.BusinessException("'active' is required.");
        return productService.setActive(productId, active, (User) auth.getPrincipal());
    }

    @Operation(summary = "Seller/Admin: Delete a product (soft delete)")
    @DeleteMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteProduct(@PathVariable String productId, Authentication auth) {
        productService.deleteProduct(productId, (User) auth.getPrincipal());
        return ResponseEntity.noContent().build();
    }

    // --- ADMIN MODERATION ---

    @Operation(summary = "Admin: Moderation queue / product search",
            description = "status: PENDING_REVIEW (default view), PUBLISHED, REJECTED or empty for all. q: product name, seller name or product ID.")
    @GetMapping("/admin")
    @PreAuthorize("hasAnyAuthority('MANAGE_MARKET', 'MANAGE_SYSTEM')")
    @SecurityRequirement(name = "bearerAuth")
    public Page<ProductResponse> moderationQueue(@RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String q,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return productService.moderationQueue(status, q, page, size);
    }

    @Operation(summary = "Admin: Approve (publish) a product")
    @PutMapping("/{productId}/approve")
    @PreAuthorize("hasAnyAuthority('MANAGE_MARKET', 'MANAGE_SYSTEM')")
    @SecurityRequirement(name = "bearerAuth")
    public ProductResponse approve(@PathVariable String productId, Authentication auth) {
        return productService.approve(productId, (User) auth.getPrincipal());
    }

    @Operation(summary = "Admin: Reject a product", description = "Body: { \"reason\": \"...\" } (required).")
    @PutMapping("/{productId}/reject")
    @PreAuthorize("hasAnyAuthority('MANAGE_MARKET', 'MANAGE_SYSTEM')")
    @SecurityRequirement(name = "bearerAuth")
    public ProductResponse reject(@PathVariable String productId, @RequestBody Map<String, String> body, Authentication auth) {
        return productService.reject(productId, body.get("reason"), (User) auth.getPrincipal());
    }
}
