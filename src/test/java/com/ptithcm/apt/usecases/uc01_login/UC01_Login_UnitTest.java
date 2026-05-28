package com.ptithcm.apt.usecases.uc01_login;

import com.ptithcm.apt.dto.request.LoginRequest;
import com.ptithcm.apt.dto.response.TokenResponse;
import com.ptithcm.apt.entity.Role;
import com.ptithcm.apt.entity.Token;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.service.JwtService;
import com.ptithcm.apt.service.ResidentService;
import com.ptithcm.apt.service.TokenService;
import com.ptithcm.apt.service.UserService;
import com.ptithcm.apt.service.impl.AuthServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Tích hợp Mockito vào JUnit 5 để hỗ trợ tạo các đối tượng giả (Mock)
@ExtendWith(MockitoExtension.class)
@DisplayName("UC01_Login_UnitTest - Kiểm thử Đơn vị (DTO & Service)")
public class UC01_Login_UnitTest {

    @Nested
    @DisplayName("1. Kiểm thử Validation Đầu vào (DTO)")
    class DTOValidationTest {
        
        // Sử dụng Validator thuần của Java để kiểm tra các ràng buộc (@NotBlank, @Size)
        private static Validator validator;

        @BeforeAll
        static void setUpValidator() {
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        }

        @Test
        @DisplayName("TC-04: BVA - Password = 6 ký tự (Hợp lệ)")
        void testValidLoginRequest() {
            LoginRequest request = new LoginRequest("admin@gmail.com", "123456");
            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
            
            // Đảm bảo không có lỗi validation nào được sinh ra với dữ liệu hợp lệ
            assertTrue(violations.isEmpty(), "Dữ liệu hợp lệ không nên sinh ra lỗi validation");
        }

        // Sử dụng ParameterizedTest để test cùng 1 logic với nhiều bộ dữ liệu khác nhau
        @ParameterizedTest(name = "{0} - {1}")
        @CsvSource({
                "TC-03, BVA - Password < 6 ký tự, admin@gmail.com, 12345, Password phải có ít nhất 6 ký tự",
                "TC-05, EP - Bỏ trống Email (username), '', 123456, Username không được để trống",
                "TC-06, EP - Bỏ trống Password, admin@gmail.com, '', Password không được để trống"
        })
        @DisplayName("Kiểm tra Validation của LoginRequest với dữ liệu không hợp lệ")
        void testInvalidLoginRequest(String testCaseId, String description, String username, String password, String expectedMessage) {
            // Chuyển đổi chuỗi rỗng thành null để mô phỏng chính xác trường hợp request không gửi trường này lên
            if (username != null && username.isEmpty()) username = null;
            if (password != null && password.isEmpty()) password = null;

            LoginRequest request = new LoginRequest(username, password);
            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

            // Chắc chắn phải có lỗi validation xảy ra
            assertFalse(violations.isEmpty(), "Dữ liệu không hợp lệ phải sinh ra lỗi validation");
            
            // Kiểm tra xem trong danh sách lỗi có chứa thông báo mong đợi hay không
            boolean hasExpectedError = violations.stream()
                    .anyMatch(v -> v.getMessage().equals(expectedMessage));
            assertTrue(hasExpectedError, "Thông báo lỗi phải khớp với mong đợi: " + expectedMessage);
        }
    }

    @Nested
    @DisplayName("2. Kiểm thử Logic Nghiệp vụ (AuthServiceImpl)")
    class ServiceLogicTest {

        // @Mock: Tạo ra các đối tượng giả để không phải kết nối với Database thật
        @Mock
        private UserService userService;
        @Mock
        private TokenService tokenService;
        @Mock
        private JwtService jwtService;
        @Mock
        private AuthenticationManager authenticationManager;
        @Mock
        private ResidentService residentService;
        @Mock
        private HttpServletRequest httpRequest;

        // @InjectMocks: Bơm tất cả các đối tượng @Mock ở trên vào bên trong AuthServiceImpl
        @InjectMocks
        private AuthServiceImpl authService;

        private User mockUser;
        private Role mockRole;

        @BeforeEach
        void setUp() {
            // Gán giá trị cho biến môi trường được inject qua @Value
            ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L); // 7 days
            
            // Thiết lập dữ liệu User giả lập dùng chung cho các Test Case
            mockRole = new Role();
            mockRole.setRoleName("ROLE_USER");

            mockUser = new User();
            mockUser.setId(1L);
            mockUser.setUsername("user@gmail.com");
            mockUser.setPassword("encoded_password");
            mockUser.setRole(mockRole);
        }

        @Test
        @DisplayName("TC-07 & TC-08: Branch - AuthenticationManager Exception (Sai tài khoản hoặc mật khẩu)")
        void testLogin_AuthenticationFailed() {
            LoginRequest request = new LoginRequest("user@gmail.com", "wrong_pass");

            // Giả lập hành vi: Ép AuthenticationManager ném ra Exception khi được gọi
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác"));

            // Đảm bảo hàm login sẽ bị ngắt và ném ra đúng Exception mong đợi
            assertThrows(BadCredentialsException.class, () -> authService.login(request, httpRequest));

            // Xác minh (Verify) rằng luồng code đã dừng lại và KHÔNG gọi xuống các hàm bên dưới
            verify(userService, never()).findByUsername(anyString());
            verify(jwtService, never()).generateAccessToken(any(User.class));
        }

        @Test
        @DisplayName("TC-09: Branch - validTokens.isEmpty() == TRUE (Đăng nhập lần đầu trên MOBILE)")
        void testLogin_Success_NoOldTokens() {
            LoginRequest request = new LoginRequest("user@gmail.com", "pass_dung");

            // Giả lập (Mock) các hàm trả về dữ liệu thành công
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
            when(userService.findByUsername("user@gmail.com")).thenReturn(mockUser);
            when(jwtService.generateAccessToken(mockUser)).thenReturn("access_token_123");
            when(jwtService.generateRefreshToken(mockUser)).thenReturn("refresh_token_123");
            when(httpRequest.getHeader("User-Agent")).thenReturn("Android App");
            
            // Giả lập trạng thái: User chưa có token nào hợp lệ trước đó
            when(tokenService.findAllValidByUserAndDevice(mockUser.getId(), "MOBILE"))
                    .thenReturn(Collections.emptyList());
            when(residentService.findNameByUserId(mockUser.getId())).thenReturn(Optional.of("Nguyen Van A"));

            // Chạy hàm cần test
            TokenResponse response = authService.login(request, httpRequest);

            // So sánh kết quả trả về
            assertNotNull(response);
            assertEquals("access_token_123", response.accessToken());
            assertEquals("refresh_token_123", response.refreshToken());
            assertEquals("Nguyen Van A", response.user().residentName());

            // Xác minh luồng logic: Không được gọi hàm thu hồi token, chỉ được gọi hàm lưu token mới 1 lần
            verify(tokenService, never()).revokeAllAndSave(anyList());
            verify(tokenService, times(1)).save(any(Token.class));
        }

        @Test
        @DisplayName("TC-10: Branch - validTokens.isEmpty() == FALSE (Đã có token cũ, cần thu hồi)")
        void testLogin_Success_HasOldTokens() {
            LoginRequest request = new LoginRequest("user@gmail.com", "pass_dung");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
            when(userService.findByUsername("user@gmail.com")).thenReturn(mockUser);
            when(jwtService.generateAccessToken(mockUser)).thenReturn("access_token_456");
            when(jwtService.generateRefreshToken(mockUser)).thenReturn("refresh_token_456");
            when(httpRequest.getHeader("User-Agent")).thenReturn("Android App");

            // Giả lập trạng thái: User đang có 1 token cũ còn hạn (cần bị thu hồi)
            Token oldToken = Token.builder()
                    .id(1L).user(mockUser).token("old_hashed_token")
                    .deviceInfo("MOBILE").expiresAt(LocalDateTime.now().plusDays(1)).revoked(false).build();
            List<Token> oldTokens = new ArrayList<>();
            oldTokens.add(oldToken);

            when(tokenService.findAllValidByUserAndDevice(mockUser.getId(), "MOBILE"))
                    .thenReturn(oldTokens);
            when(residentService.findNameByUserId(mockUser.getId())).thenReturn(Optional.of("Nguyen Van A"));

            TokenResponse response = authService.login(request, httpRequest);

            assertNotNull(response);
            assertEquals("access_token_456", response.accessToken());

            // Xác minh luồng logic: Code phải rẽ nhánh vào hàm thu hồi Token cũ (revokeAllAndSave)
            verify(tokenService, times(1)).revokeAllAndSave(oldTokens);
            verify(tokenService, times(1)).save(any(Token.class));
        }
    }
}
