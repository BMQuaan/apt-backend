package com.ptithcm.apt.usecases.uc03_change_password;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptithcm.apt.dto.request.ChangePasswordRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UC03_ChangePassword_IntegrationTest - Kiểm thử Tích hợp (Black Box API)")
public class UC03_ChangePassword_IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("TC-02: Đổi mật khẩu thất bại - Bị chặn do chưa Đăng nhập (HTTP 401)")
    // Không dùng @WithMockUser ở đây để giả lập việc không có Token
    void testChangePassword_Unauthorized() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("currentPass", "newPass123");

        mockMvc.perform(patch("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Spring Security Filter phải nhảy ra chặn ngay lập tức
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@gmail.com", roles = "USER")
    @DisplayName("TC-05: Đổi mật khẩu thất bại - Nhập sai mật khẩu hiện tại (HTTP 400)")
    void testChangePassword_WrongOldPassword() throws Exception {
        // "admin@gmail.com" phải tồn tại trong Database thực tế
        ChangePasswordRequest request = new ChangePasswordRequest("sai_mat_khau_cu", "newPass123");

        mockMvc.perform(patch("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // AuthServiceImpl sẽ bắt và ném lỗi 400
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @ParameterizedTest(name = "{0} - {1}")
    @CsvSource({
            "TC-03, Bỏ trống mật khẩu hiện tại, '', newPass123",
            "TC-04, Mật khẩu mới quá ngắn, currentPass, 12345",
            "TC-XX, Bỏ trống mật khẩu mới, currentPass, ''"
    })
    @WithMockUser(username = "admin@gmail.com", roles = "USER")
    @DisplayName("Validation HTTP 400 - API Đổi Mật Khẩu")
    void testChangePassword_ValidationFail(String testId, String description, String oldPassword, String newPassword) throws Exception {
        if (oldPassword != null && oldPassword.isEmpty()) oldPassword = null;
        if (newPassword != null && newPassword.isEmpty()) newPassword = null;

        ChangePasswordRequest request = new ChangePasswordRequest(oldPassword, newPassword);

        mockMvc.perform(patch("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Tầng Controller bắt dính @Valid và trả về 400 Bad Request
                .andExpect(status().isBadRequest());
    }
}
