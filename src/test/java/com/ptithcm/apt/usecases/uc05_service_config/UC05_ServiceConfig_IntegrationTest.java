package com.ptithcm.apt.usecases.uc05_service_config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ptithcm.apt.dto.request.ServicePriceUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UC05_ServiceConfig_IntegrationTest - Kiểm thử Tích hợp (Phân quyền API)")
public class UC05_ServiceConfig_IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Module này cho phép Jackson map được kiểu LocalDate của Java 8
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @WithMockUser(username = "resident@gmail.com", roles = "USER")
    @DisplayName("TC-01: User thông thường cố tình gọi API Cập nhật giá -> Bị chặn (HTTP 403 Forbidden)")
    void testUpdatePrice_ForbiddenForNormalUser() throws Exception {
        ServicePriceUpdateRequest request = new ServicePriceUpdateRequest("WATER", new BigDecimal("20000"), LocalDate.now().plusMonths(1));

        mockMvc.perform(post("/api/v1/service-configs/upcoming")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Kiểm chứng: Spring Security sẽ nhận diện role USER và tát văng ra mã lỗi 403
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bạn không có quyền truy cập tài nguyên này."));
    }

    @Test
    @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
    @DisplayName("TC-02: Admin gọi API nhưng truyền sai định dạng -> Qua được chốt 403, bị chặn ở 400")
    void testUpdatePrice_AdminPassesSecurityButFailsValidation() throws Exception {
        // Cố tình truyền một cái Request sai định dạng (Giá bằng 0)
        ServicePriceUpdateRequest request = new ServicePriceUpdateRequest("WATER", BigDecimal.ZERO, LocalDate.now().plusMonths(1));

        mockMvc.perform(post("/api/v1/service-configs/upcoming")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Kiểm chứng: Nhờ mang thẻ ADMIN, người dùng đã vượt qua được lớp Security.
                // Nhưng ngay sau đó, Annotation @Valid ở Controller sẽ quăng lỗi 400 Bad Request.
                // Điều này chứng minh chức năng phân quyền đã hoạt động hoàn hảo!
                .andExpect(status().isBadRequest());
    }
}
