package com.doch.User.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.doch.User.model.Token;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Integer> {


    @Query("""
        select t from Token t inner join User u on t.user.id = u.id
        where t.user.id = :userId and t.loggedOut = false
        """)
    List<Token> findAllAccessTokensByUser(String userId);

    Optional<Token> findFirstByAccessTokenOrderByIdDesc(String token);

    Optional<Token> findFirstByRefreshTokenOrderByIdDesc(String token);

    // Older rows may hold duplicate token strings, so never expect a unique match.
    default Optional<Token> findByAccessToken(String token) {
        return findFirstByAccessTokenOrderByIdDesc(token);
    }

    default Optional<Token> findByRefreshToken(String token) {
        return findFirstByRefreshTokenOrderByIdDesc(token);
    }
}
