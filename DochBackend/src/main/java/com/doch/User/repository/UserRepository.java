package com.doch.User.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.doch.User.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmailAndIsDeletedFalse(String email);
    @Query("SELECT u FROM User u WHERE u.isDeleted = true")
    List<User> findAllDeletedUsers();
    Optional<User> findByEmail(String email);
    Optional<User> findByUserName(String username);

}
