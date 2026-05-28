package com.ptithcm.apt.usecases.uc13_view_bill;

import com.ptithcm.apt.dto.response.PageResponse;
import com.ptithcm.apt.dto.response.UserBillDetailResponse;
import com.ptithcm.apt.dto.response.UserBillListResponse;
import com.ptithcm.apt.dto.response.UserRentInvoiceDetailResponse;
import com.ptithcm.apt.dto.response.UserRentInvoiceListResponse;
import com.ptithcm.apt.enums.BillStatus;
import com.ptithcm.apt.enums.RentStatus;
import com.ptithcm.apt.service.BillService;
import com.ptithcm.apt.service.RentInvoiceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("UC13_ViewBill_IntegrationTest - Kiểm thử Tích hợp Xem Hóa Đơn")
public class UC13_ViewBill_IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BillService billService;

    @MockitoBean
    private RentInvoiceService rentInvoiceService;

    @Test
    @WithMockUser(username = "residentA", roles = "USER")
    @DisplayName("TC-01/09: Lấy danh sách hóa đơn dịch vụ của tôi thành công - Filter hợp lệ")
    void testGetMyBills_Success() throws Exception {
        UserBillListResponse billRes = UserBillListResponse.builder()
                .id(99L)
                .apartmentName("101")
                .billingMonth(5)
                .billingYear(2026)
                .totalAmount(new BigDecimal("350000"))
                .status(BillStatus.UNPAID)
                .viewerRole("HEAD")
                .build();

        Page<UserBillListResponse> page = new PageImpl<>(Collections.singletonList(billRes));
        when(billService.getMyBills(eq(5), eq(2026), eq(1L), eq(BillStatus.UNPAID), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/bills/me")
                        .param("month", "5")
                        .param("year", "2026")
                        .param("apartmentId", "1")
                        .param("status", "UNPAID")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(99))
                .andExpect(jsonPath("$.data.content[0].viewerRole").value("HEAD"));
    }

    @Test
    @WithMockUser(username = "tenantC", roles = "USER")
    @DisplayName("TC-03: Lấy danh sách hóa đơn thuê nhà của tôi thành công")
    void testGetMyRentInvoices_Success() throws Exception {
        UserRentInvoiceListResponse rentRes = UserRentInvoiceListResponse.builder()
                .id(12L)
                .apartmentName("101")
                .billingMonth(5)
                .billingYear(2026)
                .rentAmount(new BigDecimal("5000000"))
                .status(RentStatus.UNPAID)
                .viewerRole("TENANT")
                .build();

        Page<UserRentInvoiceListResponse> page = new PageImpl<>(Collections.singletonList(rentRes));
        when(rentInvoiceService.getMyRentInvoices(eq(5), eq(2026), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/rent-invoices/me")
                        .param("month", "5")
                        .param("year", "2026")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(12))
                .andExpect(jsonPath("$.data.content[0].viewerRole").value("TENANT"));
    }

    @Test
    @WithMockUser(username = "residentA", roles = "USER")
    @DisplayName("TC-05: Xem chi tiết hóa đơn dịch vụ hợp lệ")
    void testGetMyBillDetail_Success() throws Exception {
        UserBillDetailResponse detail = UserBillDetailResponse.builder()
                .id(99L)
                .apartmentName("101")
                .billingMonth(5)
                .billingYear(2026)
                .totalAmount(new BigDecimal("350000"))
                .status(BillStatus.UNPAID)
                .build();

        when(billService.getMyBillDetailById(99L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/bills/me/99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(99))
                .andExpect(jsonPath("$.data.apartmentName").value("101"));
    }

    @Test
    @WithMockUser(username = "tenantC", roles = "USER")
    @DisplayName("TC-07: Xem chi tiết hóa đơn thuê nhà hợp lệ")
    void testGetMyRentInvoiceDetail_Success() throws Exception {
        UserRentInvoiceDetailResponse detail = UserRentInvoiceDetailResponse.builder()
                .id(12L)
                .apartmentName("101")
                .billingMonth(5)
                .billingYear(2026)
                .rentAmount(new BigDecimal("5000000"))
                .status(RentStatus.UNPAID)
                .build();

        when(rentInvoiceService.getMyRentInvoiceDetailById(12L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/rent-invoices/me/12")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.apartmentName").value("101"));
    }

    @Test
    @WithMockUser(username = "residentA", roles = "USER")
    @DisplayName("TC-08: BVA - Truyền sai Enum Status cho Bill list")
    void testGetMyBills_Fail_InvalidStatusEnum() throws Exception {
        mockMvc.perform(get("/api/v1/bills/me")
                        .param("status", "PROCESSING")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
