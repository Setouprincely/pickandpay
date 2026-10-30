package com.doch.Catalog.repository;

import java.util.List;
import java.util.Optional;

import com.doch.Catalog.model.ProductPublicationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.doch.Catalog.model.Product;

public interface ProductRepository extends JpaRepository<Product, String>{

    List<Product> findBySellerId(String sellerId);
    List<Product> findBySellerIsNotNull();
    List<Product> findAllByCategoriesIdAndSellerIsNotNull(String categoryId);
    List<Product> findAllByCategoriesId(String categoryId);

    /** Marketplace catalogue: published, active listings of verified sellers, optionally filtered. */
    @Query("""
        SELECT DISTINCT p FROM Product p
        JOIN p.seller s
        LEFT JOIN p.categories c
        WHERE p.publicationStatus = com.doch.Catalog.model.ProductPublicationStatus.PUBLISHED
          AND p.isActivity = true
          AND s.isVerified = true
          AND (:categoryId IS NULL OR c.id = :categoryId)
          AND (:sellerId IS NULL OR s.id = :sellerId)
          AND (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
        ORDER BY p.createdAt DESC
        """)
    List<Product> findPublicCatalogue(@Param("q") String q,
                                      @Param("categoryId") String categoryId,
                                      @Param("sellerId") String sellerId);

    @Query("""
        SELECT p FROM Product p
        WHERE (:status IS NULL OR p.publicationStatus = :status)
          AND (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(p.seller.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR p.id = :q)
        """)
    Page<Product> searchForModeration(@Param("status") ProductPublicationStatus status,
                                      @Param("q") String q,
                                      Pageable pageable);

    long countByPublicationStatus(ProductPublicationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") String id);
}