package com.ptithcm.apt.usecases.uc11_resident;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ptithcm.apt.dto.request.UpdateResidentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UC04_Resident_IntegrationTest - Kiểm thử API và Phân quyền")
public class UC11_Resident_IntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Hỗ trợ parse kiểu LocalDate sang JSON
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @WithMockUser(roles = "ACCOUNTANT") // Kế toán không có quyền update cư dân
    @DisplayName("TC-09: Phân quyền - Tài khoản không đủ quyền (403 Forbidden)")
    void testUpdateResident_Unauthorized() throws Exception {
        // Sử dụng Setter do DTO không có AllArgsConstructor
        UpdateResidentRequest request = new UpdateResidentRequest();
        request.setFullName("Nguyen Van A");
        request.setDob(LocalDate.of(1990, 1, 1));
        request.setPhone("0987654321");
        request.setCitizenIdentity("123456789012");
        request.setEmail("a@gmail.com");

        mockMvc.perform(put("/api/v1/residents/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // Spring Security chặn lại
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-11: BVA - CCCD thiếu ký tự (11 số)")
    void testUpdateResident_Fail_CCCD_Length() throws Exception {
        UpdateResidentRequest request = new UpdateResidentRequest();
        request.setFullName("Nguyen Van A");
        request.setDob(LocalDate.of(1990, 1, 1));
        request.setPhone("0987654321");
        // Cố tình truyền CCCD chỉ có 11 số (Sai regex)
        request.setCitizenIdentity("12345678901");
        request.setEmail("a@gmail.com");

        mockMvc.perform(put("/api/v1/residents/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Validator chặn lại
    }
}