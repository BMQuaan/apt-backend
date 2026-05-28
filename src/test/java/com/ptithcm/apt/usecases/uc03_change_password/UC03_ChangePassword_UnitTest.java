package com.ptithcm.apt.usecases.uc03_change_password;

import com.ptithcm.apt.dto.request.ChangePasswordRequest;
import com.ptithcm.apt.entity.User;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Tích hợp Mockito vào JUnit 5
@ExtendWith(MockitoExtension.class)
@DisplayName("UC03_ChangePassword_UnitTest - Kiểm thử Đơn vị (DTO & Service)")
public class UC03_ChangePassword_UnitTest {

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
                "TC-03, Bỏ trống mật khẩu hiện tại, '', newPass123, Mật khẩu hiện tại không được để trống",
                "TC-04, Mật khẩu mới < 6 ký tự, currentPass, 12345, Mật khẩu phải có ít nhất 6 ký tự",
                "TC-XX, Bỏ trống mật khẩu mới, currentPass, '', Mật khẩu mới không được để trống"
        })
        @DisplayName("Validation ChangePasswordRequest (Lỗi HTTP 400)")
        void testChangePasswordRequest_ValidationFail(String testCaseId, String description, String oldPassword, String newPassword, String expectedMessage) {
            // Xử lý chuỗi rỗng từ CsvSource
            if (oldPassword != null && oldPassword.isEmpty()) oldPassword = null;
            if (newPassword != null && newPassword.isEmpty()) newPassword = null;

            ChangePasswordRequest request = new ChangePasswordRequest(oldPassword, newPassword);
            Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);
            
            // Đảm bảo phải có lỗi văng ra
            assertFalse(violations.isEmpty(), "Phải báo lỗi validation");
            // Đảm bảo lỗi trả về đúng với yêu cầu thiết kế
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals(expectedMessage)));
        }
    }

    @Nested
    @DisplayName("2. Kiểm thử Logic Nghiệp vụ (AuthServiceImpl)")
    class ServiceLogicTest {

        @Mock
        private UserService userService;
        @Mock
        private PasswordEncoder passwordEncoder;
        @Mock
        private TokenService tokenService;

        @InjectMocks
        private AuthServiceImpl authService;

        private User mockUser;

        @BeforeEach
        void setUp() {
            // Giả lập thông tin User lấy từ Database
            mockUser = new User();
            mockUser.setId(1L);
            mockUser.setUsername("user@gmail.com");
            mockUser.setPassword("encoded_current_pass");

            // Giả lập Security Context (Vì hàm changePassword gọi SecurityContextHolder để lấy User đang đăng nhập)
            Authentication authentication = mock(Authentication.class);
            when(authentication.getName()).thenReturn("user@gmail.com");
            
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);
        }

        @Test
        @DisplayName("TC-05: Nhánh - Nhập sai mật khẩu hiện tại")
        void testChangePassword_WrongOldPassword() {
            ChangePasswordRequest request = new ChangePasswordRequest("sai_pass_roi", "newPass123");
            when(userService.findByUsername("user@gmail.com")).thenReturn(mockUser);
            
            // Giả lập Password Encoder báo mật khẩu không khớp
            when(passwordEncoder.matches("sai_pass_roi", "encoded_current_pass")).thenReturn(false);

            // Bắt lỗi và so sánh message
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.changePassword(request));
            assertEquals("Mật khẩu hiện tại không chính xác.", exception.getMessage());
            
            // Đảm bảo hàm lưu mật khẩu không bị gọi
            verify(userService, never()).save(any());
        }

        @Test
        @DisplayName("TC-06: Nhánh - Mật khẩu mới trùng với mật khẩu hiện tại")
        void testChangePassword_NewPasswordSameAsOld() {
            ChangePasswordRequest request = new ChangePasswordRequest("currentPass", "currentPass");
            when(userService.findByUsername("user@gmail.com")).thenReturn(mockUser);
            
            // Pass cũ nhập đúng
            when(passwordEncoder.matches("currentPass", "encoded_current_pass")).thenReturn(true);
            // Nhưng Pass mới cũng khớp y chang Pass cũ trong DB
            when(passwordEncoder.matches("currentPass", "encoded_current_pass")).thenReturn(true);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.changePassword(request));
            assertEquals("Mật khẩu mới không được trùng với mật khẩu hiện tại.", exception.getMessage());
        }

        @Test
        @DisplayName("TC-07: Nhánh - Đổi mật khẩu THÀNH CÔNG")
        void testChangePassword_Success() {
            ChangePasswordRequest request = new ChangePasswordRequest("currentPass", "newPass123");
            when(userService.findByUsername("user@gmail.com")).thenReturn(mockUser);
            
            // Pass cũ đúng
            when(passwordEncoder.matches("currentPass", "encoded_current_pass")).thenReturn(true);
            // Pass mới khác pass cũ
            when(passwordEncoder.matches("newPass123", "encoded_current_pass")).thenReturn(false);
            
            when(passwordEncoder.encode("newPass123")).thenReturn("encoded_new_pass_123");

            // Chạy hàm
            authService.changePassword(request);

            // Xác minh Pass mới đã được set vào User và lưu xuống DB
            assertEquals("encoded_new_pass_123", mockUser.getPassword());
            verify(userService, times(1)).save(mockUser);

            // Xác minh hệ thống đã gọi hàm quét và thu hồi TOÀN BỘ Token cũ của User này (Bắt đăng nhập lại)
            verify(tokenService, times(1)).findAllValidByUserAndDevice(mockUser.getId(), "WEB");
            verify(tokenService, times(1)).findAllValidByUserAndDevice(mockUser.getId(), "MOBILE");
        }
    }
}
