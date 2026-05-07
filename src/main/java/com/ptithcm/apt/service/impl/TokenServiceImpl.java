package com.ptithcm.apt.service.impl;

import com.ptithcm.apt.entity.Token;
import com.ptithcm.apt.repository.TokenRepository;
import com.ptithcm.apt.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final TokenRepository tokenRepository;

    @Override
    public List<Token> findAllValidByUserAndDevice(Long userId, String deviceType) {
        return tokenRepository.findAllValidTokenByUserAndDeviceType(userId, deviceType);
    }

    @Override
    public void revokeAllAndSave(List<Token> tokens) {
        tokens.forEach(token -> token.setRevoked(true));
        tokenRepository.saveAll(tokens);
    }

    @Override
    public void save(Token token) {
        tokenRepository.save(token);
    }

    @Override
    public Optional<Token> findByToken(String hashedToken) {
        return tokenRepository.findByToken(hashedToken);
    }
}
