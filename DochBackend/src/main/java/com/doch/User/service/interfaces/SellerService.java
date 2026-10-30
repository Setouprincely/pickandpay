package com.doch.User.service.interfaces;

import java.util.List;

import com.doch.User.dto.SellerResponse;
import com.doch.User.dto.Request.SellerRequest;

public interface SellerService {

    public String createSeller(SellerRequest request, com.doch.User.model.User currentUser);

    public List<SellerResponse> getSellerByUserId(String userId);

    public List<SellerResponse> getSellersByCity(String city);

    public List<SellerResponse> getAllSellers();

    public void deleteSeller(String sellerId);

    // public Seller updateSellerAvailability(String sellerId, Boolean isAvailable);

    public SellerResponse updateSellerVerification(String sellerId, Boolean isVerified, String note);

    public SellerResponse getSellerById(String sellerId);
}
