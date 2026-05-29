package com.ptithcm.apt.usecases.uc14_manage_bill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptithcm.apt.dto.request.UpdateBillStatusRequest;
import com.ptithcm.apt.dto.request.UpdateRentInvoiceStatusRequest;
import com.ptithcm.apt.dto.response.AdminBillDetailResponse;
import com.ptithcm.apt.dto.response.AdminBillListResponse;
import com.ptithcm.apt.dto.response.AdminRentInvoiceDetailResponse;
import com.ptithcm.apt.dto.response.AdminRentInvoiceListResponse;
import com.ptithcm.apt.dto.response.UpdateBillStatusResponse;
import com.ptithcm.apt.dto.response.UpdateRentInvoiceStatusResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("UC14_ManageBill_IntegrationTest - Kiểm thử Tích hợp Quản Lý Hóa Đơn")
public class UC14_ManageBill_IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private BillService billService;

    @MockitoBean
    private RentInvoiceService rentInvoiceService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-01: Admin cập nhật trạng thái hóa đơn dịch vụ thành công")
    void testUpdateBillStatus_Success() throws Exception {
        UpdateBillStatusRequest request = new UpdateBillStatusRequest(BillStatus.PAID);
        UpdateBillStatusResponse response = UpdateBillStatusResponse.builder()
                .id(1L)
                .status(BillStatus.PAID)
                .confirmedBy("admin")
                .build();

        when(billService.updateBillStatus(eq(1L), any(UpdateBillStatusRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/bills/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-02: Admin cập nhật trạng thái hóa đơn thuê nhà thành công")
    void testUpdateRentInvoiceStatus_Success() throws Exception {
        UpdateRentInvoiceStatusRequest request = new UpdateRentInvoiceStatusRequest(RentStatus.PAID);
        UpdateRentInvoiceStatusResponse response = UpdateRentInvoiceStatusResponse.builder()
                .id(1L)
                .status(RentStatus.PAID)
                .confirmedBy("admin")
                .build();

        when(rentInvoiceService.updateRentInvoiceStatus(eq(1L), any(UpdateRentInvoiceStatusRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/rent-invoices/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-09: Admin xem chi tiết hóa đơn dịch vụ thành công")
    void testGetBillDetailByAdmin_Success() throws Exception {
        AdminBillDetailResponse detail = AdminBillDetailResponse.builder()
                .id(1L)
                .apartmentName("P101")
                .billingMonth(5)
                .billingYear(2026)
                .totalAmount(new BigDecimal("350000"))
                .status(BillStatus.UNPAID)
                .build();

        when(billService.getBillDetailByAdmin(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/bills/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.apartmentName").value("P101"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-10: Admin xem chi tiết hóa đơn thuê nhà thành công")
    void testGetRentInvoiceDetailByAdmin_Success() throws Exception {
        AdminRentInvoiceDetailResponse detail = AdminRentInvoiceDetailResponse.builder()
                .id(1L)
                .apartmentName("P101")
                .billingMonth(5)
                .billingYear(2026)
                .rentAmount(new BigDecimal("5000000"))
                .status(RentStatus.UNPAID)
                .build();

        when(rentInvoiceService.getRentInvoiceDetailByAdmin(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/rent-invoices/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.apartmentName").value("P101"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-11: Admin lọc danh sách hóa đơn theo tên phòng")
    void testGetBillsByAdmin_FilterRoomNumber() throws Exception {
        AdminBillListResponse billRes = AdminBillListResponse.builder()
                .id(1L)
                .apartmentName("P101")
                .billingMonth(5)
                .billingYear(2026)
                .totalAmount(new BigDecimal("350000"))
                .status(BillStatus.UNPAID)
                .build();

        Page<AdminBillListResponse> page = new PageImpl<>(Collections.singletonList(billRes));
        when(billService.getBillsByAdmin(any(), any(), any(), any(), eq("P101"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/bills")
                        .param("roomNumber", "P101")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].apartmentName").value("P101"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-12: Admin lọc danh sách hóa đơn thuê nhà theo tháng năm")
    void testGetRentInvoicesByAdmin_FilterMonthYear() throws Exception {
        AdminRentInvoiceListResponse rentRes = AdminRentInvoiceListResponse.builder()
                .id(1L)
                .apartmentName("P101")
                .billingMonth(5)
                .billingYear(2026)
                .rentAmount(new BigDecimal("5000000"))
                .status("UNPAID")
                .build();

        Page<AdminRentInvoiceListResponse> page = new PageImpl<>(Collections.singletonList(rentRes));
        when(rentInvoiceService.getRentInvoiceListByAdmin(eq(5), eq(2026), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/rent-invoices")
                        .param("month", "5")
                        .param("year", "2026")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].billingMonth").value(5))
                .andExpect(jsonPath("$.data.content[0].billingYear").value(2026));
    }
}
