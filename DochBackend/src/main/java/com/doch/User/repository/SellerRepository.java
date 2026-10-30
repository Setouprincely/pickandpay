package com.doch.User.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.doch.User.model.Seller;

public interface SellerRepository extends JpaRepository<Seller, String> {

    List<Seller> findByUserId(String userId);
    // List<Seller> findByIsVerified(Boolean isVerified);
    // List<Seller> findByIsAvailable(Boolean isAvailable);
    List<Seller> findByCity(String city);
}
