package com.ptithcm.apt.service.impl;

import com.ptithcm.apt.entity.Otp;
import com.ptithcm.apt.repository.OtpRepository;
import com.ptithcm.apt.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final OtpRepository otpRepository;

    @Override
    public long countByEmailSince(String email, LocalDateTime since) {
        return otpRepository.countByEmailAndCreatedAtAfter(email, since);
    }

    @Override
    public Optional<Otp> findTopActiveByEmail(String email) {
        return otpRepository.findTopByEmailAndIsUsedFalseAndIsRevokedFalseOrderByCreatedAtDesc(email);
    }

    @Override
    public Otp save(Otp otp) {
        return otpRepository.save(otp);
    }

    @Override
    public Optional<Otp> findByResetTokenAndValid(String hashedToken) {
        return otpRepository.findByResetTokenAndIsUsedFalseAndIsRevokedFalse(hashedToken);
    }
}
