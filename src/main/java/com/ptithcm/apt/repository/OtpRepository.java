package com.ptithcm.apt.repository;

import com.ptithcm.apt.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findTopByEmailAndIsUsedFalseAndIsRevokedFalseOrderByCreatedAtDesc(String email);

    Optional<Otp> findByResetTokenAndIsUsedFalseAndIsRevokedFalse(String resetToken);

    @Query("SELECT COUNT(o) FROM Otp o WHERE o.email = :email AND o.createdAt >= :timeLimit")
    long countByEmailAndCreatedAtAfter(@Param("email") String email, @Param("timeLimit") LocalDateTime timeLimit);
}