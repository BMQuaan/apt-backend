package com.ptithcm.apt.usecases.uc02_forgot_password;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptithcm.apt.dto.request.ForgotPasswordRequest;
import com.ptithcm.apt.dto.request.ResetPasswordRequest;
import com.ptithcm.apt.dto.request.VerifyOtpRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UC02_ForgotPassword_IntegrationTest - Kiểm thử Tích hợp (Black Box API)")
public class UC02_ForgotPassword_IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==========================================
    // 1. API GỬI MÃ OTP (/forgot-password)
    // ==========================================

    @Test
    @DisplayName("Gửi mã OTP thất bại - Email không tồn tại trong hệ thống (HTTP 404)")
    void testForgotPassword_EmailNotFound() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("email_khong_ton_tai@gmail.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Spring Boot Exception Handler sẽ bắt NotFoundException và trả về HTTP 404
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @ParameterizedTest(name = "{0} - {1}")
    @CsvSource({
            "TC-14, Bỏ trống Email, ''",
            "TC-13, Sai định dạng Email, admin_gmail.com"
    })
    @DisplayName("Validation HTTP 400 - API Gửi mã OTP")
    void testForgotPassword_ValidationFail(String testId, String description, String email) throws Exception {
        if (email != null && email.isEmpty()) email = null;
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // 2. API XÁC THỰC MÃ OTP (/verify-otp)
    // ==========================================

    @Test
    @DisplayName("Xác thực OTP thất bại - Nhập sai mã (HTTP 400)")
    void testVerifyOtp_WrongOtp() throws Exception {
        // Lưu ý: "admin@gmail.com" phải có thật dưới DB và đã được gửi OTP trước đó, nhưng mã OTP "999999" là sai
        VerifyOtpRequest request = new VerifyOtpRequest("admin@gmail.com", "999999");

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Logic Backend ném RuntimeException -> Chuyển thành HTTP 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Validation HTTP 400 - API Xác thực OTP (Bỏ trống mã)")
    void testVerifyOtp_ValidationFail() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest("admin@gmail.com", "");

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // 3. API ĐẶT LẠI MẬT KHẨU (/reset-password)
    // ==========================================

    @Test
    @DisplayName("Đặt lại mật khẩu thất bại - Token bảo mật bị sai/hết hạn (HTTP 400)")
    void testResetPassword_InvalidToken() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("token_gian_diep", "newpass123");

        mockMvc.perform(patch("/api/v1/auth/reset-password") // Lưu ý: Hàm này dùng @PatchMapping
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @ParameterizedTest(name = "{0} - {1}")
    @CsvSource({
            "TC-17, Bỏ trống mật khẩu mới, ''",
            "TC-16, Mật khẩu quá ngắn, 12345"
    })
    @DisplayName("Validation HTTP 400 - API Đặt lại mật khẩu")
    void testResetPassword_ValidationFail(String testId, String description, String newPassword) throws Exception {
        if (newPassword != null && newPassword.isEmpty()) newPassword = null;
        ResetPasswordRequest request = new ResetPasswordRequest("valid_token", newPassword);

        mockMvc.perform(patch("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
