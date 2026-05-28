package com.ptithcm.apt.usecases.uc08_contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ptithcm.apt.dto.request.ContractRequest;
import com.ptithcm.apt.service.ContractService;

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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UC08_Contract_IntegrationTest - Kiểm thử Tích hợp (Black Box API)")
public class UC08_Contract_IntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        @MockitoBean
        private ContractService contractService;

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("TC-01: Lập hợp đồng thành công (Hợp lệ)")
        void testCreateContract_Success() throws Exception {
                ContractRequest request = new ContractRequest(
                                "Nguyen Van A", LocalDate.of(1995, 5, 20), "0123456789",
                                "123456789012", "nguyenvana@gmail.com", 1L, "TENANT",
                                new BigDecimal("5000000"), new BigDecimal("5000000"),
                                LocalDate.now(), LocalDate.now().plusYears(1));

                mockMvc.perform(post("/api/v1/contracts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                                .andExpect(status().isCreated());
        }

        @ParameterizedTest(name = "{0} - {1}")
        @CsvSource({
                        "TC-02, BVA - Số điện thoại thiếu, 123456, 123456789012, test@gmail.com",
                        "TC-03, BVA - CCCD sai định dạng, 0123456789, 123, test@gmail.com",
                        "TC-04, EP - Email sai định dạng, 0123456789, 123456789012, email_khong_hop_le"
        })
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Kiểm tra HTTP 400 khi dữ liệu DTO sai định dạng")
        void testCreateContract_Fail_Validation(String testCaseId, String desc, String phone, String cccd, String email)
                        throws Exception {
                ContractRequest request = new ContractRequest(
                                "Nguyen Van A", LocalDate.of(1995, 5, 20), phone, cccd, email,
                                1L, "TENANT", new BigDecimal("5000"), new BigDecimal("5000"),
                                LocalDate.now(), LocalDate.now().plusYears(1));

                mockMvc.perform(post("/api/v1/contracts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }
}