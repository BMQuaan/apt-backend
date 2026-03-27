package com.ptithcm.apt.service.impl;

import com.ptithcm.apt.dto.request.LoginRequest;
import com.ptithcm.apt.dto.request.RefreshTokenRequest;
import com.ptithcm.apt.dto.response.TokenResponse;
import com.ptithcm.apt.entity.Token;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.exception.NotFoundException;
import com.ptithcm.apt.repository.TokenRepository;
import com.ptithcm.apt.repository.UserRepository;
import com.ptithcm.apt.service.AuthService;
import com.ptithcm.apt.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Override
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        String jwtToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        revokeAllUserTokens(user);
        saveUserToken(user, refreshToken);

        TokenResponse.UserInfo userInfo = TokenResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole().getRoleName())
                .build();

        return TokenResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .user(userInfo)
                .build();
    }

    @Override
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        final String rawRefreshToken = request.getRefreshToken();

        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new RuntimeException("Refresh token không được để trống");
        }

        String username;
        try {
            username = jwtService.extractUsername(rawRefreshToken);
        } catch (Exception e) {
            throw new RuntimeException("Refresh token không hợp lệ", e);
        }

        if (username != null) {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new NotFoundException("User không tồn tại"));

            String hashedToken = hashToken(rawRefreshToken);

            boolean isTokenValidInDb = tokenRepository.findByToken(hashedToken)
                    .map(t -> !t.getRevoked() && t.getExpiresAt().isAfter(LocalDateTime.now()))
                    .orElse(false);

            if (jwtService.isTokenValid(rawRefreshToken, user) && isTokenValidInDb) {
                String accessToken = jwtService.generateAccessToken(user);

                return TokenResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(rawRefreshToken)
                        .build();
            }
        }
        throw new RuntimeException("Refresh token đã hết hạn hoặc bị thu hồi");
    }

    private void saveUserToken(User user, String rawRefreshToken) {
        String hashedToken = hashToken(rawRefreshToken);

        Token token = Token.builder()
                .user(user)
                .token(hashedToken)
                .expiresAt(LocalDateTime.now().plus(refreshTokenExpiration, ChronoUnit.MILLIS))
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        List<Token> validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (validUserTokens.isEmpty()) return;

        validUserTokens.forEach(token -> token.setRevoked(true));
        tokenRepository.saveAll(validUserTokens);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Không tìm thấy thuật toán mã hóa", e);
        }
    }
}