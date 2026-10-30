package com.doch.User.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.doch.User.model.Role;

public interface RoleRepository extends JpaRepository<Role, String>{
    Optional<Role> findByIsDeletedFalse();
    Optional<Role> findByNameAndIsDeletedFalse(String name);
    Optional<Role> findByIdAndIsDeletedFalse(String id);
    List<Role> findByPermissionsIdAndIsDeletedFalse(String permissionId);
    @Query("SELECT r FROM Role r WHERE r.isDeleted = false")
    List<Role> findAllActiveRoles();
    boolean existsByName(String name);
    List<Role> findByNameNotInAndIsDeletedFalse(List<String> roleNames);
}
