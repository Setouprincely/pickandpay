package com.doch.Catalog.service.Interface;

import com.doch.Catalog.dto.request.ProductRequest;
import com.doch.Catalog.dto.response.ProductResponse;
import com.doch.User.model.User;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request, User seller);

    ProductResponse createProductWithImage(MultipartFile image, ProductRequest request, User seller);

    ProductResponse updateProduct(ProductRequest request, String id, User currentUser);

    /** Marketplace catalogue: only published, active products of verified sellers. */
    List<ProductResponse> getPublicCatalogue(String query, String categoryId, String sellerId);

    void deleteProduct(String id, User currentUser);

    String addProductImage(MultipartFile image, User seller);

    /** Public product page; non-visible products are only returned to their seller or staff. */
    ProductResponse getProduct(String productId, User viewerOrNull);

    /** Every product of the logged-in seller, whatever its publication status. */
    List<ProductResponse> getMyProducts(User seller);

    ProductResponse setActive(String productId, boolean active, User currentUser);

    Page<ProductResponse> moderationQueue(String status, String query, int page, int size);

    ProductResponse approve(String productId, User admin);

    ProductResponse reject(String productId, String reason, User admin);
}