package com.ptithcm.apt.usecases.uc14_manage_bill;

import com.ptithcm.apt.dto.request.UpdateBillStatusRequest;
import com.ptithcm.apt.dto.request.UpdateRentInvoiceStatusRequest;
import com.ptithcm.apt.dto.response.UpdateBillStatusResponse;
import com.ptithcm.apt.dto.response.UpdateRentInvoiceStatusResponse;
import com.ptithcm.apt.entity.Bill;
import com.ptithcm.apt.entity.RentInvoice;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.enums.BillStatus;
import com.ptithcm.apt.enums.RentStatus;
import com.ptithcm.apt.exception.NotFoundException;
import com.ptithcm.apt.mapper.BillMapper;
import com.ptithcm.apt.mapper.RentInvoiceMapper;
import com.ptithcm.apt.repository.BillRepository;
import com.ptithcm.apt.repository.RentInvoiceRepository;
import com.ptithcm.apt.service.UserService;
import com.ptithcm.apt.service.impl.BillServiceImpl;
import com.ptithcm.apt.service.impl.RentInvoiceServiceImpl;
import com.ptithcm.apt.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UC14_ManageBill_UnitTest - Kiểm thử Đơn vị Quản Lý Hóa Đơn")
public class UC14_ManageBill_UnitTest {

    @Mock
    private BillRepository billRepository;
    @Mock
    private RentInvoiceRepository rentInvoiceRepository;
    @Mock
    private BillMapper billMapper;
    @Mock
    private RentInvoiceMapper rentInvoiceMapper;
    @Mock
    private UserService userService;

    @InjectMocks
    private BillServiceImpl billService;

    @InjectMocks
    private RentInvoiceServiceImpl rentInvoiceService;

    private User currentUser;
    private Bill bill;
    private RentInvoice rentInvoice;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(2L);
        currentUser.setUsername("adminUser");

        bill = new Bill();
        bill.setId(1L);
        bill.setStatus(BillStatus.UNPAID);

        rentInvoice = new RentInvoice();
        rentInvoice.setId(1L);
        rentInvoice.setStatus(RentStatus.UNPAID);
    }

    @Test
    @DisplayName("TC-01: Admin duyệt hóa đơn dịch vụ UNPAID thành PAID thành công")
    void testUpdateBillStatus_Success() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUsername).thenReturn("adminUser");
            when(userService.findByUsername("adminUser")).thenReturn(currentUser);
            when(billRepository.findById(1L)).thenReturn(Optional.of(bill));

            UpdateBillStatusRequest req = new UpdateBillStatusRequest(BillStatus.PAID);
            UpdateBillStatusResponse mockRes = UpdateBillStatusResponse.builder()
                    .id(1L)
                    .status(BillStatus.PAID)
                    .confirmedBy("adminUser")
                    .build();
            when(billMapper.toUpdateBillStatusResponse(bill)).thenReturn(mockRes);

            UpdateBillStatusResponse response = billService.updateBillStatus(1L, req);

            assertNotNull(response);
            assertEquals(BillStatus.PAID, bill.getStatus());
            assertNotNull(bill.getPaidAt());
            assertEquals(currentUser, bill.getConfirmedBy());
            verify(billRepository, times(1)).save(bill);
        }
    }

    @Test
    @DisplayName("TC-02: Admin duyệt hóa đơn thuê UNPAID thành PAID thành công")
    void testUpdateRentInvoiceStatus_Success() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUsername).thenReturn("adminUser");
            when(userService.findByUsername("adminUser")).thenReturn(currentUser);
            when(rentInvoiceRepository.findById(1L)).thenReturn(Optional.of(rentInvoice));

            UpdateRentInvoiceStatusRequest req = new UpdateRentInvoiceStatusRequest(RentStatus.PAID);
            UpdateRentInvoiceStatusResponse mockRes = UpdateRentInvoiceStatusResponse.builder()
                    .id(1L)
                    .status(RentStatus.PAID)
                    .confirmedBy("adminUser")
                    .build();
            when(rentInvoiceMapper.toUpdateBillStatusResponse(rentInvoice)).thenReturn(mockRes);

            UpdateRentInvoiceStatusResponse response = rentInvoiceService.updateRentInvoiceStatus(1L, req);

            assertNotNull(response);
            assertEquals(RentStatus.PAID, rentInvoice.getStatus());
            assertNotNull(rentInvoice.getPaidAt());
            assertEquals(currentUser, rentInvoice.getConfirmedBy());
            verify(rentInvoiceRepository, times(1)).save(rentInvoice);
        }
    }

    @Test
    @DisplayName("TC-03: Branch - Duyệt hóa đơn dịch vụ không tồn tại")
    void testUpdateBillStatus_Fail_NotFound() {
        when(billRepository.findById(9999L)).thenReturn(Optional.empty());
        UpdateBillStatusRequest req = new UpdateBillStatusRequest(BillStatus.PAID);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> billService.updateBillStatus(9999L, req));

        assertTrue(ex.getMessage().contains("Không tìm thấy hóa đơn"));
    }

    @Test
    @DisplayName("TC-04: Branch - Chuyển hóa đơn dịch vụ sang trạng thái khác ngoài PAID")
    void testUpdateBillStatus_Fail_InvalidNewStatus() {
        when(billRepository.findById(1L)).thenReturn(Optional.of(bill));
        UpdateBillStatusRequest req = new UpdateBillStatusRequest(BillStatus.LATE);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> billService.updateBillStatus(1L, req));

        assertTrue(ex.getMessage().contains("Chỉ hỗ trợ chuyển trạng thái sang ĐÃ THANH TOÁN"));
    }

    @Test
    @DisplayName("TC-05: Branch - Hóa đơn dịch vụ đã được PAID từ trước")
    void testUpdateBillStatus_Fail_AlreadyPaid() {
        bill.setStatus(BillStatus.PAID);
        when(billRepository.findById(1L)).thenReturn(Optional.of(bill));
        UpdateBillStatusRequest req = new UpdateBillStatusRequest(BillStatus.PAID);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> billService.updateBillStatus(1L, req));

        assertTrue(ex.getMessage().contains("Hóa đơn đã được thanh toán"));
    }

    @Test
    @DisplayName("TC-06: Branch - Duyệt hóa đơn thuê không tồn tại")
    void testUpdateRentInvoiceStatus_Fail_NotFound() {
        when(rentInvoiceRepository.findById(9999L)).thenReturn(Optional.empty());
        UpdateRentInvoiceStatusRequest req = new UpdateRentInvoiceStatusRequest(RentStatus.PAID);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> rentInvoiceService.updateRentInvoiceStatus(9999L, req));

        assertTrue(ex.getMessage().contains("Không tìm thấy hóa đơn thuê nhà"));
    }

    @Test
    @DisplayName("TC-07: Branch - Chuyển hóa đơn thuê sang trạng thái khác ngoài PAID")
    void testUpdateRentInvoiceStatus_Fail_InvalidNewStatus() {
        when(rentInvoiceRepository.findById(1L)).thenReturn(Optional.of(rentInvoice));
        UpdateRentInvoiceStatusRequest req = new UpdateRentInvoiceStatusRequest(RentStatus.LATE);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> rentInvoiceService.updateRentInvoiceStatus(1L, req));

        assertTrue(ex.getMessage().contains("Hệ thống chỉ hỗ trợ cập nhật trạng thái sang ĐÃ THANH TOÁN"));
    }

    @Test
    @DisplayName("TC-08: Branch - Hóa đơn thuê đã được PAID từ trước")
    void testUpdateRentInvoiceStatus_Fail_AlreadyPaid() {
        rentInvoice.setStatus(RentStatus.PAID);
        when(rentInvoiceRepository.findById(1L)).thenReturn(Optional.of(rentInvoice));
        UpdateRentInvoiceStatusRequest req = new UpdateRentInvoiceStatusRequest(RentStatus.PAID);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> rentInvoiceService.updateRentInvoiceStatus(1L, req));

        assertTrue(ex.getMessage().contains("Hóa đơn đã được thanh toán"));
    }
}
