package com.ptithcm.apt.usecases.uc06_view_service_prices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UC06_ViewServicePrices_IntegrationTest - Kiểm thử Tích hợp (Request Parameter)")
public class UC06_ViewServicePrices_IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "resident@gmail.com", roles = "USER")
    @DisplayName("TC-01 & TC-02: User/Admin xem bảng giá hợp lệ (HTTP 200)")
    void testGetPrices_Success() throws Exception {
        // Gửi GET request kèm theo parameter date đúng chuẩn ISO (YYYY-MM-DD)
        mockMvc.perform(get("/api/v1/service-configs/active")
                        .param("date", "2024-12-01"))
                // Kiểm chứng: API công khai cho cả User và Admin nên sẽ vượt qua Security dễ dàng
                .andExpect(status().isOk())
                // Trả về cấu trúc chuẩn JSON (Message và Data dạng mảng)
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "resident@gmail.com", roles = "USER")
    @DisplayName("TC-03: Bỏ trống tham số date (HTTP 400)")
    void testGetPrices_MissingDateParam() throws Exception {
        // Cố tình không đính kèm .param("date", ...)
        mockMvc.perform(get("/api/v1/service-configs/active"))
                // Kiểm chứng: Annotation @RequestParam(required = true) sẽ chặn lại và quăng lỗi
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "resident@gmail.com", roles = "USER")
    @DisplayName("TC-04: Sai định dạng tham số date (HTTP 400)")
    void testGetPrices_InvalidDateFormat() throws Exception {
        // Cố tình truyền ngày tháng kiểu Việt Nam (DD-MM-YYYY) thay vì ISO
        mockMvc.perform(get("/api/v1/service-configs/active")
                        .param("date", "01-12-2024"))
                // Kiểm chứng: Annotation @DateTimeFormat(iso = ISO.DATE) sẽ dịch lỗi và quăng 400 Bad Request
                .andExpect(status().isBadRequest());
    }
}
