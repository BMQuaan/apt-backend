package com.ptithcm.apt.usecases.uc04_logout;

import com.ptithcm.apt.config.CustomLogoutHandler;
import com.ptithcm.apt.entity.Token;
import com.ptithcm.apt.repository.TokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

// Tích hợp Mockito vào JUnit 5
@ExtendWith(MockitoExtension.class)
@DisplayName("UC04_Logout_UnitTest - Kiểm thử Đơn vị (CustomLogoutHandler)")
public class UC04_Logout_UnitTest {

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CustomLogoutHandler customLogoutHandler;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        // Dùng MockHttpServletRequest từ Spring Test để giả lập 1 request web
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("TC-01: Nhánh - Token HỢP LỆ có tồn tại trong Database")
    void testLogout_ValidTokenInDatabase() {
        String rawTokenValue = "valid_token_value";
        request.addHeader("Authorization", "Bearer " + rawTokenValue);

        // Giả lập tìm thấy Token trong DB
        Token mockToken = new Token();
        mockToken.setRevoked(false); // Đang còn hiệu lực
        
        // Lưu ý: Source code thực tế đang gọi findByToken(rawToken) chứ không phải hashedToken.
        // Test script mô phỏng đúng hành vi hiện tại của code.
        when(tokenRepository.findByToken(rawTokenValue)).thenReturn(Optional.of(mockToken));

        customLogoutHandler.logout(request, response, authentication);

        // Xác minh: Token phải bị cập nhật trạng thái Revoked = true
        assertTrue(mockToken.getRevoked(), "Token phải bị đánh dấu là đã thu hồi (Revoked = true)");
        
        // Xác minh: Lệnh save() xuống Database phải được gọi đúng 1 lần
        verify(tokenRepository, times(1)).save(mockToken);
    }

    @Test
    @DisplayName("TC-02: Nhánh - Token KHÔNG TỒN TẠI trong Database")
    void testLogout_TokenNotInDatabase() {
        String rawTokenValue = "fake_token_value";
        request.addHeader("Authorization", "Bearer " + rawTokenValue);

        // Giả lập DB không tìm thấy token này (có thể là token rác kẻ gian gửi lên)
        when(tokenRepository.findByToken(rawTokenValue)).thenReturn(Optional.empty());

        customLogoutHandler.logout(request, response, authentication);

        // Xác minh: Không được gọi lệnh lưu DB
        verify(tokenRepository, never()).save(any());
        // Code vẫn chạy mượt mà không văng lỗi Exception
    }
}
