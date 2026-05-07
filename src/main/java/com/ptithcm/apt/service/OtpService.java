package com.ptithcm.apt.service;

import com.ptithcm.apt.entity.Otp;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpService {
    long countByEmailSince(String email, LocalDateTime since);
    Optional<Otp> findTopActiveByEmail(String email);
    Otp save(Otp otp);
    Optional<Otp> findByResetTokenAndValid(String hashedToken);
}
