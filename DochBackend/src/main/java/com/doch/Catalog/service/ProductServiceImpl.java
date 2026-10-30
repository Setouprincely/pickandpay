package com.doch.Catalog.service;

import com.doch.Catalog.dto.request.ProductRequest;
import com.doch.Catalog.dto.response.ProductResponse;
import com.doch.Catalog.model.Category;
import com.doch.Catalog.model.Product;
import com.doch.Catalog.model.ProductImage;
import com.doch.Catalog.model.ProductPublicationStatus;
import com.doch.Catalog.repository.CategoryRepository;
import com.doch.Catalog.repository.ProductImageRepository;
import com.doch.Catalog.repository.ProductRepository;
import com.doch.Catalog.service.Interface.ProductService;
import com.doch.Notification.model.NotificationType;
import com.doch.Notification.service.NotificationService;
import com.doch.User.model.Seller;
import com.doch.User.model.User;
import com.doch.User.repository.SellerRepository;
import com.doch.common.ImageUploadValidator;
import com.doch.common.Mapper;
import com.doch.common.audit.AuditService;
import com.doch.common.exceptions.BusinessException;
import com.doch.common.exceptions.ResourceNotFoundException;
import com.doch.common.settings.PricingSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Set<String> CATALOG_ADMIN = Set.of("MANAGE_MARKET", "MANAGE_SYSTEM", "DELETE_PRODUCT");

    private final StoreMediaUsecase storeMediaUsecase;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final SellerRepository sellerRepository;
    private final PricingSettingsService settingsService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final Mapper converter;

    private Seller sellerOf(User user) {
        return sellerRepository.findByUserId(user.getId()).stream().findFirst()
                .orElseThrow(() -> new BusinessException("You must have a seller profile to manage products."));
    }

    private ProductPublicationStatus initialStatus() {
        return Boolean.FALSE.equals(settingsService.getOrCreateSettings().getProductModerationRequired())
                ? ProductPublicationStatus.PUBLISHED : ProductPublicationStatus.PENDING_REVIEW;
    }

    private void applyFields(Product product, ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        product.setName(request.getProductName().trim());
        product.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        product.setPrice(request.getPrice());
        product.setCurrency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency().trim().toUpperCase() : "XAF");
        product.setSKU(request.getSKU());
        product.setStockQuantity(request.getStockQuantity());
        product.setWeight(request.getWeight().trim());
        product.setWeightKg(WeightParser.toKilograms(request.getWeight()));
        product.setLengthCm(request.getLengthCm());
        product.setWidthCm(request.getWidthCm());
        product.setHeightCm(request.getHeightCm());
        product.setCategories(new ArrayList<>(List.of(category)));
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request, User sellerUser) {
        Seller seller = sellerOf(sellerUser);
        Product product = Product.builder()
                .seller(seller)
                .isActivity(true)
                .images(new ArrayList<>())
                .publicationStatus(initialStatus())
                .build();
        applyFields(product, request);

        if (request.getImagerIds() != null && !request.getImagerIds().isEmpty()) {
            List<ProductImage> images = productImageRepository.findAllById(request.getImagerIds());
            if (images.size() != request.getImagerIds().size()) {
                throw new ResourceNotFoundException("One or more image IDs were not found. Upload images first via /api/product/addPicture.");
            }
            for (ProductImage img : images) {
                if (img.getProduct() != null) {
                    throw new BusinessException("An image is already attached to another product.");
                }
                img.setProduct(product);
                product.getImages().add(img);
            }
        }
        Product saved = productRepository.save(product);
        afterSubmission(saved, sellerUser);
        return converter.toProductResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse createProductWithImage(MultipartFile image, ProductRequest request, User sellerUser) {
        Seller seller = sellerOf(sellerUser);
        ImageUploadValidator.validate(image, "Product photo");
        Product product = Product.builder()
                .seller(seller)
                .isActivity(true)
                .images(new ArrayList<>())
                .publicationStatus(initialStatus())
                .build();
        applyFields(product, request);

        String stored = storeImage(image);
        ProductImage productImage = ProductImage.builder()
                .contentType(image.getContentType())
                .imageUrl(stored)
                .names(stored)
                .product(product)
                .build();
        product.getImages().add(productImage);

        Product saved = productRepository.save(product);
        afterSubmission(saved, sellerUser);
        return converter.toProductResponse(saved);
    }

    private void afterSubmission(Product product, User sellerUser) {
        auditService.record(sellerUser, "PRODUCT_CREATED", "PRODUCT", product.getId(),
                product.getName() + " (" + product.getPublicationStatus() + ")");
        if (product.getPublicationStatus() == ProductPublicationStatus.PENDING_REVIEW) {
            String msg = String.format("New product '%s' from %s awaits review.", product.getName(), product.getSeller().getName());
            notificationService.sendNotificationToRole("ROLE_MARKET_ADMIN", NotificationType.SYSTEM_ANNOUNCEMENT, msg);
            notificationService.sendNotificationToRole("ROLE_SUPER_ADMIN", NotificationType.SYSTEM_ANNOUNCEMENT, msg);
        }
        if (!Boolean.TRUE.equals(product.getSeller().getIsVerified())) {
            notificationService.sendNotification(sellerUser, NotificationType.SYSTEM_ANNOUNCEMENT,
                    "Product '" + product.getName() + "' was saved. It will be visible to customers once your seller account is verified"
                            + (product.getPublicationStatus() == ProductPublicationStatus.PENDING_REVIEW ? " and the listing is approved." : "."));
        }
    }

    private String storeImage(MultipartFile image) {
        try {
            String key = "products/" + UUID.randomUUID() + ImageUploadValidator.extensionFor(image);
            return storeMediaUsecase.storeInMinio(image.getBytes(), key, image.getContentType());
        } catch (Exception e) {
            log.error("Product image storage failed", e);
            throw new BusinessException("Image upload failed. Please try again.");
        }
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(ProductRequest request, String id, User currentUser) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        assertOwnerOrAdmin(product, currentUser);
        double oldPrice = product.getPrice();
        applyFields(product, request);
        if (product.getPublicationStatus() == ProductPublicationStatus.REJECTED) {
            product.setPublicationStatus(initialStatus());
            product.setRejectionReason(null);
        }
        if (request.getImagerIds() != null && !request.getImagerIds().isEmpty()) {
            for (ProductImage img : productImageRepository.findAllById(request.getImagerIds())) {
                if (img.getProduct() == null) {
                    img.setProduct(product);
                    product.getImages().add(img);
                }
            }
        }
        Product saved = productRepository.save(product);
        auditService.record(currentUser, "PRODUCT_UPDATED", "PRODUCT", saved.getId(),
                String.format("%s, price %.0f -> %.0f, stock %d", saved.getName(), oldPrice, saved.getPrice(), saved.getStockQuantity()));
        return converter.toProductResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getPublicCatalogue(String query, String categoryId, String sellerId) {
        return converter.toProductResponseList(productRepository.findPublicCatalogue(
                StringUtils.hasText(query) ? query.trim() : null,
                StringUtils.hasText(categoryId) ? categoryId : null,
                StringUtils.hasText(sellerId) ? sellerId : null));
    }

    @Override
    @Transactional
    public void deleteProduct(String id, User currentUser) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        assertOwnerOrAdmin(product, currentUser);
        product.setActivity(false);
        product.setIsDeleted(true);
        productRepository.save(product);
        auditService.record(currentUser, "PRODUCT_DELETED", "PRODUCT", product.getId(), product.getName());
    }

    @Override
    @Transactional
    public String addProductImage(MultipartFile image, User seller) {
        sellerOf(seller);
        ImageUploadValidator.validate(image, "Product photo");
        String stored = storeImage(image);
        ProductImage productImage = productImageRepository.save(ProductImage.builder()
                .contentType(image.getContentType())
                .imageUrl(stored)
                .names(stored)
                .build());
        return productImage.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(String productId, User viewer) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        if (!product.isPubliclyVisible() && (viewer == null || !isOwnerOrAdmin(product, viewer))) {
            throw new ResourceNotFoundException("Product not found: " + productId);
        }
        return converter.toProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getMyProducts(User seller) {
        return converter.toProductResponseList(productRepository.findBySellerId(sellerOf(seller).getId()));
    }

    @Override
    @Transactional
    public ProductResponse setActive(String productId, boolean active, User currentUser) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        assertOwnerOrAdmin(product, currentUser);
        product.setActivity(active);
        productRepository.save(product);
        auditService.record(currentUser, active ? "PRODUCT_ACTIVATED" : "PRODUCT_DEACTIVATED", "PRODUCT", product.getId(), product.getName());
        return converter.toProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> moderationQueue(String status, String query, int page, int size) {
        ProductPublicationStatus parsed = null;
        if (StringUtils.hasText(status)) {
            try {
                parsed = ProductPublicationStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid status: " + status + ". Use PENDING_REVIEW, PUBLISHED or REJECTED.");
            }
        }
        return productRepository.searchForModeration(parsed, StringUtils.hasText(query) ? query.trim() : null,
                        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(converter::toProductResponse);
    }

    @Override
    @Transactional
    public ProductResponse approve(String productId, User admin) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setPublicationStatus(ProductPublicationStatus.PUBLISHED);
        product.setRejectionReason(null);
        product.setReviewedAt(LocalDateTime.now());
        product.setReviewedBy(admin.getId());
        productRepository.save(product);
        auditService.record(admin, "PRODUCT_APPROVED", "PRODUCT", product.getId(), product.getName());
        notificationService.sendNotification(product.getSeller().getUser(), NotificationType.SYSTEM_ANNOUNCEMENT,
                "Your product '" + product.getName() + "' was approved"
                        + (Boolean.TRUE.equals(product.getSeller().getIsVerified()) ? " and is now live." : ". It will go live once your seller account is verified."));
        return converter.toProductResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse reject(String productId, String reason, User admin) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException("A rejection reason is required.");
        }
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setPublicationStatus(ProductPublicationStatus.REJECTED);
        product.setRejectionReason(reason.trim());
        product.setReviewedAt(LocalDateTime.now());
        product.setReviewedBy(admin.getId());
        productRepository.save(product);
        auditService.record(admin, "PRODUCT_REJECTED", "PRODUCT", product.getId(), product.getName() + ": " + reason.trim());
        notificationService.sendNotification(product.getSeller().getUser(), NotificationType.SYSTEM_ANNOUNCEMENT,
                "Your product '" + product.getName() + "' was not approved: " + reason.trim() + ". Edit it to resubmit.");
        return converter.toProductResponse(product);
    }

    private boolean isOwnerOrAdmin(Product product, User user) {
        boolean admin = user.getAuthorities().stream().anyMatch(a -> CATALOG_ADMIN.contains(a.getAuthority()));
        boolean owner = product.getSeller() != null && product.getSeller().getUser() != null
                && product.getSeller().getUser().getId().equals(user.getId());
        return admin || owner;
    }

    private void assertOwnerOrAdmin(Product product, User user) {
        if (!isOwnerOrAdmin(product, user)) {
            throw new AccessDeniedException("You can only manage your own products.");
        }
    }
}
