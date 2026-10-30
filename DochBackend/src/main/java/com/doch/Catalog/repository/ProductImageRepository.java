package com.doch.Catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.doch.Catalog.model.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, String> {

}
