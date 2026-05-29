package com.ptithcm.apt.usecases.uc12_create_bill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptithcm.apt.dto.request.CreateBillRequest;
import com.ptithcm.apt.dto.response.BillSummaryResponse;
import com.ptithcm.apt.service.BillService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("UC12_CreateBill_IntegrationTest - Kiểm thử Tích hợp Lập Hóa Đơn")
public class UC12_CreateBill_IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private BillService billService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-01/02: Lập hóa đơn thành công - Dữ liệu hợp lệ")
    void testCreateBill_Success() throws Exception {
        CreateBillRequest request = new CreateBillRequest(1L, 5, 2026, new BigDecimal("150.0"), new BigDecimal("60.0"));
        
        when(billService.createBill(any(CreateBillRequest.class))).thenReturn(new BillSummaryResponse(null, null, null));

        mockMvc.perform(post("/api/v1/bills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0} - {1}")
    @CsvSource({
            "TC-15, BVA - Tháng dưới ranh giới (0), 1, 0, 2026, 150.0, 60.0",
            "TC-18, BVA - Tháng trên ranh giới (13), 1, 13, 2026, 150.0, 60.0",
            "TC-19, BVA - Năm không dương (0), 1, 5, 0, 150.0, 60.0",
            "TC-21, BVA - Chỉ số điện âm (-5.0), 1, 5, 2026, -5.0, 60.0",
            "TC-22, BVA - Chỉ số nước âm (-2.0), 1, 5, 2026, 150.0, -2.0"
    })
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Kiểm tra HTTP 400 khi Dữ liệu đầu vào vi phạm Validation DTO")
    void testCreateBill_Fail_ValidationDTO(String testCaseId, String desc, Long apartmentId, Integer month, Integer year, String electricityStr, String waterStr) throws Exception {
        BigDecimal electricity = new BigDecimal(electricityStr);
        BigDecimal water = new BigDecimal(waterStr);

        CreateBillRequest request = new CreateBillRequest(apartmentId, month, year, electricity, water);

        mockMvc.perform(post("/api/v1/bills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Kiểm tra validation khi apartmentId bị null")
    void testCreateBill_Fail_NullApartmentId() throws Exception {
        CreateBillRequest request = new CreateBillRequest(null, 5, 2026, new BigDecimal("150.0"), new BigDecimal("60.0"));

        mockMvc.perform(post("/api/v1/bills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
