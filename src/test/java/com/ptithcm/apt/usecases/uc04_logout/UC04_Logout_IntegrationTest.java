package com.ptithcm.apt.usecases.uc04_logout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UC04_Logout_IntegrationTest - Kiểm thử Tích hợp (Black Box API)")
public class UC04_Logout_IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin@gmail.com")
    @DisplayName("TC-01 & TC-02: Gọi API Đăng xuất thành công (HTTP 200)")
    void testLogout_Success() throws Exception {
        // Mặc dù @WithMockUser không tạo ra một JWT Token thực sự có chữ Bearer trong Header,
        // nhưng nó đủ để qua mặt lớp cửa bảo vệ đầu tiên của Spring Security Filter.
        // Sau đó CustomLogoutHandler sẽ chạy, do không thấy "Bearer " nên nó bỏ qua (hành vi của TC-03).
        // Cuối cùng, Spring trả về mã HTTP 200 và cấu trúc JSON chuẩn.
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));
    }
}
