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
import jakarta.servlet.http.HttpServletRequest;
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
    public TokenResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        String jwtToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        String deviceType = getDeviceType(httpRequest);

        revokeTokensByDeviceType(user, deviceType);

        saveUserToken(user, refreshToken, deviceType);

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
        final String rawRefreshToken = request.refreshToken();

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
                        .build();
            }
        }
        throw new RuntimeException("Refresh token đã hết hạn hoặc bị thu hồi");
    }

    private void revokeTokensByDeviceType(User user, String deviceType) {
        List<Token> validTokens = tokenRepository.findAllValidTokenByUserAndDeviceType(user.getId(), deviceType);
        if (validTokens.isEmpty()) return;

        validTokens.forEach(token -> token.setRevoked(true));
        tokenRepository.saveAll(validTokens);
    }

    private void saveUserToken(User user, String rawRefreshToken, String deviceType) {
        String hashedToken = hashToken(rawRefreshToken);

        Token token = Token.builder()
                .user(user)
                .token(hashedToken)
                .deviceInfo(deviceType)
                .expiresAt(LocalDateTime.now().plus(refreshTokenExpiration, ChronoUnit.MILLIS))
                .revoked(false)
                .build();
        tokenRepository.save(token);
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

    private String getDeviceType(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            String ua = userAgent.toLowerCase();
            if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone") || ua.contains("ipad")) {
                return "MOBILE";
            }
        }
        return "WEB";
    }
}