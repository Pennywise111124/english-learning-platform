package com.example.englishlearningplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.englishlearningplatform.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);
}
