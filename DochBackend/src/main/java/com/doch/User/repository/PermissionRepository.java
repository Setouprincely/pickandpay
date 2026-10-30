package com.doch.User.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.doch.User.model.Permission;


public interface PermissionRepository extends JpaRepository<Permission, String>{
    List<Permission> findByIsDeletedFalse();
    Optional<Permission> findByNameAndIsDeletedFalse(String name);
    Optional<Permission> findByIdAndIsDeletedFalse(String id);
    @Query("SELECT p FROM Permission p WHERE p.isDeleted = false")
    List<Permission> findAllActivePermissions();
    boolean existsByName(String name);
    List<Permission> findAllByIdInAndIsDeletedFalse(List<String> permissionId);
    List<Permission> findAllByNameInAndIsDeletedFalse(List<String> permissions);
}
