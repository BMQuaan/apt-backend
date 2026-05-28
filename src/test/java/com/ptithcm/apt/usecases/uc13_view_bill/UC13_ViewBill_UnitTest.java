package com.ptithcm.apt.usecases.uc13_view_bill;

import com.ptithcm.apt.dto.response.UserBillDetailResponse;
import com.ptithcm.apt.dto.response.UserBillListResponse;
import com.ptithcm.apt.dto.response.UserRentInvoiceDetailResponse;
import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.entity.Bill;
import com.ptithcm.apt.entity.RentInvoice;
import com.ptithcm.apt.entity.Resident;
import com.ptithcm.apt.entity.ResidentApartment;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.enums.BillStatus;
import com.ptithcm.apt.enums.RentStatus;
import com.ptithcm.apt.mapper.BillMapper;
import com.ptithcm.apt.mapper.RentInvoiceMapper;
import com.ptithcm.apt.repository.BillRepository;
import com.ptithcm.apt.repository.RentInvoiceRepository;
import com.ptithcm.apt.service.ResidentApartmentService;
import com.ptithcm.apt.service.ResidentService;
import com.ptithcm.apt.service.UserService;
import com.ptithcm.apt.service.impl.RentInvoiceServiceImpl;
import com.ptithcm.apt.service.impl.UserBillQueryService;
import com.ptithcm.apt.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UC13_ViewBill_UnitTest - Kiểm thử Đơn vị Xem Hóa Đơn")
public class UC13_ViewBill_UnitTest {

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
    @Mock
    private ResidentService residentService;
    @Mock
    private ResidentApartmentService residentApartmentService;

    @InjectMocks
    private UserBillQueryService userBillQueryService;

    @InjectMocks
    private RentInvoiceServiceImpl rentInvoiceService;

    private User currentUser;
    private Resident currentResident;
    private Apartment apartment;
    private Bill bill;
    private RentInvoice rentInvoice;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("residentA");

        currentResident = new Resident();
        currentResident.setId(10L);
        currentResident.setFullName("Nguyen Van Resident");

        apartment = new Apartment();
        apartment.setId(1L);
        apartment.setRoomNumber("101");

        bill = new Bill();
        bill.setId(99L);
        bill.setApartment(apartment);
        bill.setBillingMonth(5);
        bill.setBillingYear(2026);
        bill.setStatus(BillStatus.UNPAID);
        bill.setTotalAmount(new BigDecimal("350000"));

        rentInvoice = new RentInvoice();
        rentInvoice.setId(12L);
        rentInvoice.setApartment(apartment);
        rentInvoice.setBillingMonth(5);
        rentInvoice.setBillingYear(2026);
        rentInvoice.setStatus(RentStatus.UNPAID);
        rentInvoice.setRentAmount(new BigDecimal("5000000"));
        rentInvoice.setTenant(currentResident);
    }

    @Test
    @DisplayName("TC-01/05: Lấy danh sách hóa đơn dịch vụ - Vai trò HEAD (Chủ hộ tự ở)")
    void testGetMyBills_AsHead() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUsername).thenReturn("residentA");
            when(userService.findByUsername("residentA")).thenReturn(currentUser);
            when(residentService.findByUserId(1L)).thenReturn(currentResident);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Bill> billPage = new PageImpl<>(Collections.singletonList(bill));
            when(billRepository.findMyBills(1L, null, null, null, null, pageable)).thenReturn(billPage);
            
            when(residentApartmentService.findActiveTenant(1L)).thenReturn(Optional.empty());

            when(residentApartmentService.existsByApartmentIdAndResidentIdAndIsHeadTrueAndIsActiveTrue(1L, 10L))
                    .thenReturn(true);

            Page<UserBillListResponse> responsePage = userBillQueryService.getMyBills(null, null, null, null, pageable);

            assertNotNull(responsePage);
            assertEquals(1, responsePage.getContent().size());
            UserBillListResponse res = responsePage.getContent().get(0);
            assertEquals("HEAD", res.viewerRole());
            assertNull(res.tenantName());
        }
    }

    @Test
    @DisplayName("TC-02/05: Lấy danh sách hóa đơn dịch vụ - Vai trò OWNER (Chủ nhà đang cho thuê)")
    void testGetMyBills_AsOwnerWithTenant() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUsername).thenReturn("residentA");
            when(userService.findByUsername("residentA")).thenReturn(currentUser);
            when(residentService.findByUserId(1L)).thenReturn(currentResident);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Bill> billPage = new PageImpl<>(Collections.singletonList(bill));
            when(billRepository.findMyBills(1L, null, null, null, null, pageable)).thenReturn(billPage);
            
            when(residentApartmentService.existsByApartmentIdAndResidentIdAndIsHeadTrueAndIsActiveTrue(1L, 10L))
                    .thenReturn(false);

            Resident tenant = new Resident();
            tenant.setFullName("Nguyen Van Tenant");
            ResidentApartment ra = new ResidentApartment();
            ra.setResident(tenant);
            ra.setApartment(apartment);
            when(residentApartmentService.findActiveTenant(1L)).thenReturn(Optional.of(ra));

            Page<UserBillListResponse> responsePage = userBillQueryService.getMyBills(null, null, null, null, pageable);

            assertNotNull(responsePage);
            assertEquals(1, responsePage.getContent().size());
            UserBillListResponse res = responsePage.getContent().get(0);
            assertEquals("OWNER", res.viewerRole());
            assertEquals("Nguyen Van Tenant", res.tenantName());
        }
    }

    @Test
    @DisplayName("TC-04: Branch - Xem chi tiết hóa đơn không thuộc quyền sở hữu")
    void testGetMyBillDetailById_Fail_AccessDenied() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUsername).thenReturn("residentA");
            when(userService.findByUsername("residentA")).thenReturn(currentUser);
            
            when(billRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userBillQueryService.getMyBillDetailById(99L));

            assertTrue(ex.getMessage().contains("Không tìm thấy hóa đơn hoặc bạn không có quyền xem"));
        }
    }

    @Test
    @DisplayName("TC-05: Branch - Xem chi tiết hóa đơn dịch vụ hợp lệ")
    void testGetMyBillDetailById_Success() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUsername).thenReturn("residentA");
            when(userService.findByUsername("residentA")).thenReturn(currentUser);
            when(billRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.of(bill));

            UserBillDetailResponse detailResponse = UserBillDetailResponse.builder()
                    .id(99L)
                    .apartmentName("101")
                    .billingMonth(5)
                    .billingYear(2026)
                    .totalAmount(new BigDecimal("350000"))
                    .status(BillStatus.UNPAID)
                    .build();
            when(billMapper.toGetMyBillDetailByIdResponse(bill)).thenReturn(detailResponse);

            UserBillDetailResponse result = userBillQueryService.getMyBillDetailById(99L);

            assertNotNull(result);
            assertEquals(99L, result.id());
            assertEquals("101", result.apartmentName());
        }
    }

    @Test
    @DisplayName("TC-06: Branch - Xem chi tiết hóa đơn thuê nhà không có quyền")
    void testGetMyRentInvoiceDetailById_Fail_AccessDenied() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUsername).thenReturn("residentA");
            when(userService.findByUsername("residentA")).thenReturn(currentUser);
            when(rentInvoiceRepository.findByIdAndUserId(12L, 1L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> rentInvoiceService.getMyRentInvoiceDetailById(12L));

            assertTrue(ex.getMessage().contains("Không tìm thấy hóa đơn thuê nhà hoặc bạn không có quyền xem"));
        }
    }

    @Test
    @DisplayName("TC-07: Branch - Xem chi tiết hóa đơn thuê nhà hợp lệ")
    void testGetMyRentInvoiceDetailById_Success() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUsername).thenReturn("residentA");
            when(userService.findByUsername("residentA")).thenReturn(currentUser);
            when(rentInvoiceRepository.findByIdAndUserId(12L, 1L)).thenReturn(Optional.of(rentInvoice));

            UserRentInvoiceDetailResponse detailResponse = UserRentInvoiceDetailResponse.builder()
                    .id(12L)
                    .apartmentName("101")
                    .billingMonth(5)
                    .billingYear(2026)
                    .rentAmount(new BigDecimal("5000000"))
                    .status(RentStatus.UNPAID)
                    .build();
            when(rentInvoiceMapper.toMyRentInvoiceDetailResponse(rentInvoice)).thenReturn(detailResponse);

            UserRentInvoiceDetailResponse result = rentInvoiceService.getMyRentInvoiceDetailById(12L);

            assertNotNull(result);
            assertEquals(12L, result.id());
            assertEquals("101", result.apartmentName());
        }
    }
}
