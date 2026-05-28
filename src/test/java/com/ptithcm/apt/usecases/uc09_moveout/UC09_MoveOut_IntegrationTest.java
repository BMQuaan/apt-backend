package com.ptithcm.apt.usecases.uc09_moveout; // Đổi package theo đúng file của bạn

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ptithcm.apt.service.ResidentService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UC09_MoveOut_IntegrationTest - Kiểm thử Tích hợp Dọn đi")
public class UC09_MoveOut_IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResidentService residentService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-01: Trigger thành công Endpoint Move Out")
    void testMoveOut_Endpoint_Authorized() throws Exception {

        mockMvc.perform(put("/api/v1/residents/1/apartments/101/move-out"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("TC-02: Phân quyền - User gọi API Move Out (bị chặn 403)")
    void testMoveOut_Endpoint_Unauthorized() throws Exception {
        mockMvc.perform(put("/api/v1/residents/1/apartments/101/move-out"))
                .andExpect(status().isForbidden());
    }
}