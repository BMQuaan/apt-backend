package com.ptithcm.apt.service;

import com.ptithcm.apt.entity.Token;

import java.util.List;
import java.util.Optional;

public interface TokenService {
    List<Token> findAllValidByUserAndDevice(Long userId, String deviceType);
    void revokeAllAndSave(List<Token> tokens);
    void save(Token token);
    Optional<Token> findByToken(String hashedToken);
}
