package com.ptithcm.apt.service;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface JwtService {

    String extractUsername(String token);
    public List<String> extractRoles(String token);

    String generateAccessToken(UserDetails userDetails);

    String generateRefreshToken(UserDetails userDetails);

    boolean isTokenValid(String token, UserDetails userDetails);
}