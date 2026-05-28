package com.ptithcm.apt.usecases.uc07_view_profile;

import com.ptithcm.apt.dto.response.ProfileDashboardResponse;
import com.ptithcm.apt.service.ProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UC07_ViewProfile_IntegrationTest - Kiểm thử Tích hợp (Security Token)")
public class UC07_ViewProfile_IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Mock tầng Service để Integration Test chỉ tập trung vào tầng API Controller & Security (HTTP Status)
    @MockitoBean
    private ProfileService profileService;

    @Test
    @DisplayName("TC-01, 02, 03: Gọi API xem thông tin cá nhân nhưng KHÔNG truyền Token (HTTP 401)")
    // Cố tình không dùng @WithMockUser ở đây
    void testGetProfile_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/profile"))
                // Hệ thống Spring Security sẽ bắt gặp endpoint này yêu cầu quyền và đá văng ra 401
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Bạn cần đăng nhập để truy cập tài nguyên này."));
    }

    @Test
    @WithMockUser(username = "resident@gmail.com", roles = "USER")
    @DisplayName("TC-08: Có Token hợp lệ -> Gọi API thành công (HTTP 200)")
    void testGetProfile_Success() throws Exception {
        // Giả lập Service trả về một cục data rỗng (Vì ta chỉ quan tâm API có mở cửa cho vào hay không)
        when(profileService.getProfileDashboard()).thenReturn(ProfileDashboardResponse.builder().build());

        mockMvc.perform(get("/api/v1/profile"))
                // Vượt qua được Spring Security nhờ Token hợp lệ (MockUser)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lấy tổng quan hồ sơ thành công"));
    }
}
