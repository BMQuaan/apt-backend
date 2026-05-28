package com.ptithcm.apt.usecases.uc10_apartment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptithcm.apt.dto.request.ApartmentRequest;
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

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UC02_Apartment_IntegrationTest - Kiểm thử Tích hợp (Black Box API)")
public class UC10_Apartment_IntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-01: Tạo căn hộ thành công (Dữ liệu hợp lệ)")
    void testCreateApartment_Success() throws Exception {

        ApartmentRequest request = new ApartmentRequest();
        request.setRoomNumber("210");
        request.setFloor(2);
        request.setArea(new BigDecimal("50.5"));
        request.setStatus("AVAILABLE");

        mockMvc.perform(
                post("/api/v1/apartments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0} - {1}")
    @CsvSource({
            "TC-02, BVA - Bỏ trống số phòng, '', 2, 50.5",
            "TC-03, BVA - Tầng bằng 0 (Min-1), '201', 0, 50.5",
            "TC-04, BVA - Diện tích bằng 0, '201', 2, 0.0"
    })
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Kiểm tra HTTP 400 khi Dữ liệu Căn hộ vi phạm Validation")
    void testCreateApartment_Fail_Validation(String testCaseId, String desc, String roomNumber, Integer floor,
            String areaStr) throws Exception {
        if (roomNumber != null && roomNumber.isEmpty())
            roomNumber = null;
        BigDecimal area = new BigDecimal(areaStr);

        ApartmentRequest request = new ApartmentRequest(roomNumber, floor, area, null);

        mockMvc.perform(post("/api/v1/apartments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                // Spring Validation tự động chặn và trả về 400 Bad Request
                .andExpect(status().isBadRequest());
    }
}