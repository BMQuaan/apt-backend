package com.ptithcm.apt.usecases.uc02_forgot_password;

import com.ptithcm.apt.dto.request.ForgotPasswordRequest;
import com.ptithcm.apt.dto.request.ResetPasswordRequest;
import com.ptithcm.apt.dto.request.VerifyOtpRequest;
import com.ptithcm.apt.entity.Otp;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.exception.NotFoundException;
import com.ptithcm.apt.service.EmailService;
import com.ptithcm.apt.service.OtpService;
import com.ptithcm.apt.service.TokenService;
import com.ptithcm.apt.service.UserService;
import com.ptithcm.apt.service.impl.AuthServiceImpl;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Tích hợp Mockito vào JUnit 5
@ExtendWith(MockitoExtension.class)
@DisplayName("UC02_ForgotPassword_UnitTest - Kiểm thử Đơn vị (DTO & Service)")
public class UC02_ForgotPassword_UnitTest {

    @Nested
    @DisplayName("1. Kiểm thử Validation Đầu vào (DTO)")
    class DTOValidationTest {

        private static Validator validator;

        @BeforeAll
        static void setUpValidator() {
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        }

        @ParameterizedTest(name = "{0} - {1}")
        @CsvSource({
                "TC-14, EP - Bỏ trống Email, '', Email không được để trống",
                "TC-13, EP - Sai định dạng Email, user@, Email không đúng định dạng"
        })
        @DisplayName("Validation ForgotPasswordRequest")
        void testForgotPasswordRequest(String testCaseId, String description, String email, String expectedMessage) {
            if (email != null && email.isEmpty()) email = null;
            ForgotPasswordRequest request = new ForgotPasswordRequest(email);
            Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);
            
            assertFalse(violations.isEmpty(), "Phải báo lỗi validation");
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals(expectedMessage)));
        }

        @Test
        @DisplayName("TC-15: EP - Bỏ trống mã OTP")
        void testVerifyOtpRequest_BlankOtp() {
            VerifyOtpRequest request = new VerifyOtpRequest("admin@gmail.com", "");
            Set<ConstraintViolation<VerifyOtpRequest>> violations = validator.validate(request);
            
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Mã OTP không được để trống")));
        }

        @ParameterizedTest(name = "{0} - {1}")
        @CsvSource({
                "TC-17, BVA - Mật khẩu mới rỗng, '', Mật khẩu mới không được để trống",
                "TC-16, BVA - Mật khẩu mới < 6 ký tự, 12345, Mật khẩu phải có ít nhất 6 ký tự"
        })
        @DisplayName("Validation ResetPasswordRequest")
        void testResetPasswordRequest(String testCaseId, String description, String newPassword, String expectedMessage) {
            if (newPassword != null && newPassword.isEmpty()) newPassword = null;
            ResetPasswordRequest request = new ResetPasswordRequest("validToken123", newPassword);
            Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);
            
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals(expectedMessage)));
        }
    }

    @Nested
    @DisplayName("2. Kiểm thử Logic Nghiệp vụ (AuthServiceImpl)")
    class ServiceLogicTest {

        @Mock
        private UserService userService;
        @Mock
        private OtpService otpService;
        @Mock
        private EmailService emailService;
        @Mock
        private PasswordEncoder passwordEncoder;
        @Mock
        private TokenService tokenService;

        @InjectMocks
        private AuthServiceImpl authService;

        private User mockUser;
        private Otp mockOtp;

        @BeforeEach
        void setUp() {
            mockUser = new User();
            mockUser.setId(1L);
            mockUser.setUsername("user@gmail.com");

            mockOtp = new Otp();
            mockOtp.setId(1L);
            mockOtp.setEmail("user@gmail.com");
            mockOtp.setOtpHash("hashed_123456");
            mockOtp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
            mockOtp.setAttemptCount(0);
            mockOtp.setIsRevoked(false);
            mockOtp.setIsUsed(false);
        }

        // ==========================================
        // BƯỚC 1: GỬI YÊU CẦU OTP
        // ==========================================

        @Test
        @DisplayName("TC-01: Nhánh - User không tồn tại (Gửi mã)")
        void testForgotPassword_UserNotFound() {
            ForgotPasswordRequest request = new ForgotPasswordRequest("sai@gmail.com");
            when(userService.findByUsername("sai@gmail.com")).thenThrow(new NotFoundException("User not found"));

            assertThrows(NotFoundException.class, () -> authService.forgotPassword(request));
            verify(otpService, never()).save(any());
        }

        @Test
        @DisplayName("TC-02: Nhánh - Yêu cầu Spam (>= 3 lần/30p)")
        void testForgotPassword_SpamRequest() {
            ForgotPasswordRequest request = new ForgotPasswordRequest("user@gmail.com");
            when(userService.findByUsername("user@gmail.com")).thenReturn(mockUser);
            // Giả lập đã gọi 3 lần
            when(otpService.countByEmailSince(eq("user@gmail.com"), any(LocalDateTime.class))).thenReturn(3L);

            RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.forgotPassword(request));
            assertTrue(exception.getMessage().contains("quá nhiều lần"));
        }

        @Test
        @DisplayName("TC-03: Nhánh - Hủy mã cũ khi tạo mã mới")
        void testForgotPassword_RevokeOldActiveOtp() {
            ForgotPasswordRequest request = new ForgotPasswordRequest("user@gmail.com");
            when(userService.findByUsername("user@gmail.com")).thenReturn(mockUser);
            when(otpService.countByEmailSince(eq("user@gmail.com"), any(LocalDateTime.class))).thenReturn(1L);
            
            // Giả lập đang có 1 mã cũ
            when(otpService.findTopActiveByEmail("user@gmail.com")).thenReturn(Optional.of(mockOtp));
            when(passwordEncoder.encode(anyString())).thenReturn("hashed_new_otp");

            authService.forgotPassword(request);

            // Kiểm tra mã cũ đã bị set revoked = true và lưu lại
            assertTrue(mockOtp.getIsRevoked());
            // Hàm lưu được gọi 2 lần (1 lần cho mã cũ, 1 lần cho mã mới)
            verify(otpService, times(2)).save(any(Otp.class));
            // Email phải được gửi
            verify(emailService, times(1)).sendHtmlEmail(eq("user@gmail.com"), anyString(), anyString(), anyMap());
        }

        // ==========================================
        // BƯỚC 2: XÁC THỰC MÃ OTP
        // ==========================================

        @Test
        @DisplayName("TC-05: Nhánh - Không tìm thấy yêu cầu OTP (Mã rác)")
        void testVerifyOtp_OtpNotFound() {
            VerifyOtpRequest request = new VerifyOtpRequest("user@gmail.com", "123456");
            when(userService.findByUsername("user@gmail.com")).thenReturn(mockUser);
            // Trả về rỗng (Không tìm thấy mã nào active)
            when(otpService.findTopActiveByEmail("user@gmail.com")).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.verifyOtp(request));
            assertTrue(exception.getMessage().contains("Không tìm thấy yêu cầu hoặc OTP đã bị hủy."));
        }

        @Test
        @DisplayName("TC-06: Nhánh - Mã OTP đã hết hạn")
        void testVerifyOtp_OtpExpired() {
            VerifyOtpRequest request = new VerifyOtpRequest("user@gmail.com", "123456");
            when(userService.findByUsername("user@gmail.com")).thenReturn(mockUser);
            
            // Cố tình chỉnh thời gian hết hạn về quá khứ (cách đây 1 phút)
            mockOtp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
            when(otpService.findTopActiveByEmail("user@gmail.com")).thenReturn(Optional.of(mockOtp));

            RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.verifyOtp(request));
            assertTrue(exception.getMessage().contains("đã hết hạn"));
            assertTrue(mockOtp.getIsRevoked(), "Mã hết hạn phải bị hủy ngay lập tức");
        }

        @Test
        @DisplayName("TC-08: Nhánh - Sai mã quá 5 lần (Khóa yêu cầu)")
        void testVerifyOtp_AttemptLimitExceeded() {
            VerifyOtpRequest request = new VerifyOtpRequest("user@gmail.com", "wrong_code");
            when(userService.findByUsername("user@gmail.com")).thenReturn(mockUser);
            
            // Giả lập đã sai 4 lần
            mockOtp.setAttemptCount(4);
            when(otpService.findTopActiveByEmail("user@gmail.com")).thenReturn(Optional.of(mockOtp));
            when(passwordEncoder.matches("wrong_code", mockOtp.getOtpHash())).thenReturn(false);

            RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.verifyOtp(request));
            assertTrue(exception.getMessage().contains("Nhập sai quá 5 lần. Yêu cầu khôi phục mật khẩu đã bị hủy."));
            // Phải bị hủy sau khi sai lần 5
            assertTrue(mockOtp.getIsRevoked());
        }

        @Test
        @DisplayName("TC-09: Nhánh - Xác thực OTP THÀNH CÔNG")
        void testVerifyOtp_Success() {
            VerifyOtpRequest request = new VerifyOtpRequest("user@gmail.com", "123456");
            when(userService.findByUsername("user@gmail.com")).thenReturn(mockUser);
            when(otpService.findTopActiveByEmail("user@gmail.com")).thenReturn(Optional.of(mockOtp));
            
            // Giả lập mật khẩu khớp
            when(passwordEncoder.matches("123456", mockOtp.getOtpHash())).thenReturn(true);

            String resetToken = authService.verifyOtp(request);

            assertNotNull(resetToken);
            assertTrue(resetToken.startsWith("reset_"));
            // Mã chưa bị hủy vì còn phải chờ bước Reset Password
            assertFalse(mockOtp.getIsRevoked());
            verify(otpService, times(1)).save(mockOtp);
        }

        // ==========================================
        // BƯỚC 3: ĐẶT LẠI MẬT KHẨU
        // ==========================================

        @Test
        @DisplayName("TC-10: Nhánh - Reset Token không hợp lệ")
        void testResetPassword_InvalidToken() {
            ResetPasswordRequest request = new ResetPasswordRequest("bad_token", "new_pass");
            when(otpService.findByResetTokenAndValid(anyString())).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.resetPassword(request));
            assertTrue(exception.getMessage().contains("không hợp lệ"));
        }

        @Test
        @DisplayName("TC-12: Nhánh - Reset Mật khẩu THÀNH CÔNG")
        void testResetPassword_Success() {
            ResetPasswordRequest request = new ResetPasswordRequest("valid_token", "new_pass");
            
            // Giả lập tìm thấy phiên đặt lại mật khẩu còn hạn
            when(otpService.findByResetTokenAndValid(anyString())).thenReturn(Optional.of(mockOtp));
            when(userService.findByUsername("user@gmail.com")).thenReturn(mockUser);
            when(passwordEncoder.encode("new_pass")).thenReturn("encoded_new_pass");

            authService.resetPassword(request);

            // Xác nhận mật khẩu User đã bị thay đổi
            assertEquals("encoded_new_pass", mockUser.getPassword());
            verify(userService, times(1)).save(mockUser);
            
            // Xác nhận phiên OTP này đã bị Đóng (IsUsed = true, IsRevoked = true)
            assertTrue(mockOtp.getIsUsed());
            assertTrue(mockOtp.getIsRevoked());
            verify(otpService, times(1)).save(mockOtp);

            // Đảm bảo phải gọi hàm xóa các phiên đăng nhập cũ trên Web và App
            verify(tokenService, times(1)).findAllValidByUserAndDevice(mockUser.getId(), "WEB");
            verify(tokenService, times(1)).findAllValidByUserAndDevice(mockUser.getId(), "MOBILE");
        }
    }
}
