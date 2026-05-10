package com.ptithcm.apt.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.ptithcm.apt.dto.request.GoogleLoginRequest;
import com.ptithcm.apt.dto.request.*;
import com.ptithcm.apt.dto.response.TokenResponse;
import com.ptithcm.apt.entity.Otp;
import com.ptithcm.apt.entity.Token;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.exception.NotFoundException;
import com.ptithcm.apt.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final TokenService tokenService;
    private final OtpService otpService;
    private final ResidentService residentService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${google.client.id}")
    private String googleClientId;

    @Override
    public TokenResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userService.findByUsername(request.username());

        String jwtToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        String deviceType = getDeviceType(httpRequest);

        revokeTokensByDeviceType(user, deviceType);
        saveUserToken(user, refreshToken, deviceType);

        return TokenResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .user(buildUserInfo(user))
                .build();
    }

    @Override
    @Transactional
    public TokenResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest httpRequest) {

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        try {
            GoogleIdToken idToken = verifier.verify(request.idToken());

            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                String email = payload.getEmail();
                String googleSubjectId = payload.getSubject();
                boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());

                if (!emailVerified) {
                    throw new RuntimeException("Email Google này chưa được xác thực.");
                }

                User user = userService.findByUsername(email);

                if (!user.getIsActive()) {
                    throw new RuntimeException("Tài khoản của bạn đã bị khóa.");
                }

                // Check Google ID Match
                if (user.getGoogleId() == null) {
                    user.setGoogleId(googleSubjectId);
                    userService.save(user);
                } else if (!user.getGoogleId().equals(googleSubjectId)) {
                    throw new RuntimeException("Tài khoản này đã được liên kết với một hồ sơ Google khác.");
                }

                String jwtToken = jwtService.generateAccessToken(user);
                String refreshToken = jwtService.generateRefreshToken(user);

                String deviceType = getDeviceType(httpRequest);

                revokeTokensByDeviceType(user, deviceType);
                saveUserToken(user, refreshToken, deviceType);

                return TokenResponse.builder()
                        .accessToken(jwtToken)
                        .refreshToken(refreshToken)
                        .user(buildUserInfo(user))
                        .build();

            } else {
                throw new IllegalArgumentException("Google ID Token không hợp lệ hoặc đã hết hạn.");
            }

        } catch (Exception e) {
            throw new RuntimeException("Xác thực Google thất bại: " + e.getMessage());
        }
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
            User user = userService.findByUsername(username);

            String hashedToken = hashToken(rawRefreshToken);

            boolean isTokenValidInDb = tokenService.findByToken(hashedToken)
                    .map(t -> !t.getRevoked() && t.getExpiresAt().isAfter(LocalDateTime.now()))
                    .orElse(false);

            if (jwtService.isTokenValid(rawRefreshToken, user) && isTokenValidInDb) {
                String accessToken = jwtService.generateAccessToken(user);

                return TokenResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(rawRefreshToken)
                        .user(buildUserInfo(user))
                        .build();
            }
        }
        throw new RuntimeException("Refresh token đã hết hạn hoặc bị thu hồi");
    }

    // =====================================================

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email();
        userService.findByUsername(email); // throws NotFoundException nếu không tồn tại

        // 3 lần / 30p
        LocalDateTime oneHourAgo = LocalDateTime.now().minusMinutes(30);
        long requestCount = otpService.countByEmailSince(email, oneHourAgo); // old:
                                                                             // otpRepository.countByEmailAndCreatedAtAfter(email,
                                                                             // oneHourAgo)
        if (requestCount >= 3) {
            throw new RuntimeException("Bạn đã yêu cầu quá nhiều lần. Vui lòng thử lại sau 30 phút.");
        }

        otpService.findTopActiveByEmail(email) // old:
                                               // otpRepository.findTopByEmailAndIsUsedFalseAndIsRevokedFalseOrderByCreatedAtDesc(email)
                .ifPresent(oldOtp -> {
                    oldOtp.setIsRevoked(true);
                    otpService.save(oldOtp);
                });

        String plainOtp = generateSecureOtp();

        Otp newOtp = Otp.builder()
                .email(email)
                .otpHash(passwordEncoder.encode(plainOtp))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        otpService.save(newOtp);

        Map<String, String> templateModel = new HashMap<>();
        templateModel.put("OTP_CODE", plainOtp);
        emailService.sendHtmlEmail(email, "Mã OTP Khôi Phục Mật Khẩu - APT", "otp-email.html", templateModel);
    }

    @Override
    @Transactional
    public String verifyOtp(VerifyOtpRequest request) {
        userService.findByUsername(request.email()); // throws NotFoundException nếu không tồn tại

        Otp activeOtp = otpService.findTopActiveByEmail(request.email()) // old:
                                                                         // otpRepository.findTopByEmailAndIsUsedFalseAndIsRevokedFalseOrderByCreatedAtDesc(request.email())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoặc OTP đã bị hủy."));

        if (activeOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            activeOtp.setIsRevoked(true);
            otpService.save(activeOtp);
            throw new RuntimeException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        // Check Hash & Anti Brute-Force
        if (!passwordEncoder.matches(request.otp(), activeOtp.getOtpHash())) {
            activeOtp.setAttemptCount(activeOtp.getAttemptCount() + 1);
            if (activeOtp.getAttemptCount() >= 5) {
                activeOtp.setIsRevoked(true);
                otpService.save(activeOtp);
                throw new RuntimeException("Nhập sai quá 5 lần. Yêu cầu khôi phục mật khẩu đã bị hủy.");
            }
            otpService.save(activeOtp);
            throw new RuntimeException(
                    "Mã OTP không chính xác. Bạn còn " + (5 - activeOtp.getAttemptCount()) + " lần thử.");
        }

        String rawResetToken = "reset_" + UUID.randomUUID().toString();
        String hashedResetToken = hashToken(rawResetToken);
        activeOtp.setResetToken(hashedResetToken);
        activeOtp.setExpiresAt(LocalDateTime.now().plusMinutes(10)); // Cho thêm 10 phút để gõ pass mới
        otpService.save(activeOtp);

        return rawResetToken;
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String hashedTokenFromUser = hashToken(request.resetToken());

        Otp activeOtp = otpService.findByResetTokenAndValid(hashedTokenFromUser) // old:
                                                                                 // otpRepository.findByResetTokenAndIsUsedFalseAndIsRevokedFalse(hashedTokenFromUser)
                .orElseThrow(() -> new RuntimeException("Phiên làm việc không hợp lệ hoặc đã bị hủy."));

        if (activeOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            activeOtp.setIsRevoked(true);
            otpService.save(activeOtp);
            throw new RuntimeException("Phiên làm việc đã hết hạn. Vui lòng thử lại từ đầu.");
        }

        User user = userService.findByUsername(activeOtp.getEmail());

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userService.save(user);

        activeOtp.setIsUsed(true);
        activeOtp.setIsRevoked(true);
        otpService.save(activeOtp);

        revokeTokensByDeviceType(user, "WEB");
        revokeTokensByDeviceType(user, "MOBILE");
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        User user = userService.findByUsername(currentUsername);

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu hiện tại.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userService.save(user);

        revokeTokensByDeviceType(user, "WEB");
        revokeTokensByDeviceType(user, "MOBILE");
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    private void revokeTokensByDeviceType(User user, String deviceType) {
        List<Token> validTokens = tokenService.findAllValidByUserAndDevice(user.getId(), deviceType); // old:
                                                                                                      // tokenRepository.findAllValidTokenByUserAndDeviceType(user.getId(),
                                                                                                      // deviceType)
        if (validTokens.isEmpty())
            return;
        tokenService.revokeAllAndSave(validTokens);
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
        tokenService.save(token);
    }

    private TokenResponse.UserInfo buildUserInfo(User user) {
        String residentName = residentService.findNameByUserId(user.getId()).orElse(null);

        return TokenResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole().getRoleName())
                .residentName(residentName)
                .build();
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
            if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone") || ua.contains("ipad")
                    || ua.contains("okhttp")) {
                return "MOBILE";
            }
        }
        return "WEB";
    }

    private String generateSecureOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}