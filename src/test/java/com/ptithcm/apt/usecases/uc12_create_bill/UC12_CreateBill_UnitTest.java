package com.ptithcm.apt.usecases.uc12_create_bill;

import com.ptithcm.apt.dto.BillValidationResult;
import com.ptithcm.apt.dto.request.CreateBillRequest;
import com.ptithcm.apt.dto.request.CreateRentInvoiceRequest;
import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.entity.MonthlyMetric;
import com.ptithcm.apt.entity.Resident;
import com.ptithcm.apt.entity.ResidentApartment;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.exception.NotFoundException;
import com.ptithcm.apt.repository.RentInvoiceRepository;
import com.ptithcm.apt.service.ApartmentService;
import com.ptithcm.apt.service.EmailService;
import com.ptithcm.apt.service.MonthlyMetricService;
import com.ptithcm.apt.service.ResidentApartmentService;
import com.ptithcm.apt.service.impl.BillValidationService;
import com.ptithcm.apt.service.impl.RentInvoiceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UC12_CreateBill_UnitTest - Kiểm thử Đơn vị Lập Hóa Đơn")
public class UC12_CreateBill_UnitTest {

    @Mock
    private ApartmentService apartmentService;
    @Mock
    private MonthlyMetricService monthlyMetricService;
    @Mock
    private RentInvoiceRepository rentInvoiceRepository;
    @Mock
    private ResidentApartmentService residentApartmentService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private BillValidationService billValidationService;

    @InjectMocks
    private RentInvoiceServiceImpl rentInvoiceService;

    private CreateBillRequest validRequest;
    private Apartment apartment;
    private MonthlyMetric lastMetric;

    @BeforeEach
    void setUp() {
        validRequest = new CreateBillRequest(1L, 5, 2026, new BigDecimal("150.0"), new BigDecimal("60.0"));
        apartment = new Apartment();
        apartment.setId(1L);
        apartment.setRoomNumber("101");
        apartment.setStatus("OWNER_OCCUPIED");

        lastMetric = new MonthlyMetric();
        lastMetric.setId(1L);
        lastMetric.setApartment(apartment);
        lastMetric.setBillingMonth(4);
        lastMetric.setBillingYear(2026);
        lastMetric.setElectricityNew(new BigDecimal("100.0"));
        lastMetric.setWaterNew(new BigDecimal("50.0"));
    }

    @Test
    @DisplayName("TC-01/05: Lập hóa đơn thành công - Không có MonthlyMetric trước đó")
    void testValidateCreateBill_Success_NoLastMetric() {
        when(apartmentService.findById(1L)).thenReturn(Optional.of(apartment));
        when(monthlyMetricService.findFirstByApartmentIdOrderByBillingYearDescBillingMonthDesc(1L))
                .thenReturn(Optional.empty());

        BillValidationResult result = billValidationService.validateCreateBill(validRequest);

        assertNotNull(result);
        assertEquals(apartment, result.apartment());
        assertEquals(BigDecimal.ZERO, result.oldElectricity());
        assertEquals(BigDecimal.ZERO, result.oldWater());
    }

    @Test
    @DisplayName("TC-01: Lập hóa đơn thành công - Có MonthlyMetric trước đó")
    void testValidateCreateBill_Success_WithLastMetric() {
        when(apartmentService.findById(1L)).thenReturn(Optional.of(apartment));
        when(monthlyMetricService.findFirstByApartmentIdOrderByBillingYearDescBillingMonthDesc(1L))
                .thenReturn(Optional.of(lastMetric));

        BillValidationResult result = billValidationService.validateCreateBill(validRequest);

        assertNotNull(result);
        assertEquals(apartment, result.apartment());
        assertEquals(new BigDecimal("100.0"), result.oldElectricity());
        assertEquals(new BigDecimal("50.0"), result.oldWater());
    }

    @Test
    @DisplayName("TC-03: Branch - Căn hộ không tồn tại trong hệ thống")
    void testValidateCreateBill_Fail_NotFoundApartment() {
        when(apartmentService.findById(1L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> billValidationService.validateCreateBill(validRequest));

        assertEquals("Không tìm thấy căn hộ", ex.getMessage());
    }

    @Test
    @DisplayName("TC-04: Branch - Không cho lập hóa đơn cho căn hộ trống (AVAILABLE)")
    void testValidateCreateBill_Fail_ApartmentAvailable() {
        apartment.setStatus("AVAILABLE");
        when(apartmentService.findById(1L)).thenReturn(Optional.of(apartment));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> billValidationService.validateCreateBill(validRequest));

        assertTrue(ex.getMessage().contains("Không thể tạo hóa đơn cho căn hộ đang ở trạng thái TRỐNG"));
    }

    @Test
    @DisplayName("TC-06: Branch - Lập hóa đơn cho năm trong quá khứ")
    void testValidateCreateBill_Fail_YearInPast() {
        CreateBillRequest requestInPast = new CreateBillRequest(1L, 5, 2025, new BigDecimal("150.0"), new BigDecimal("60.0"));
        
        when(apartmentService.findById(1L)).thenReturn(Optional.of(apartment));
        when(monthlyMetricService.findFirstByApartmentIdOrderByBillingYearDescBillingMonthDesc(1L))
                .thenReturn(Optional.of(lastMetric));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> billValidationService.validateCreateBill(requestInPast));

        assertTrue(ex.getMessage().contains("Không thể tạo hóa đơn cho khoảng thời gian đã có chỉ số hoặc trong quá khứ"));
    }

    @Test
    @DisplayName("TC-07: Branch - Lập hóa đơn trùng kỳ hoặc tháng nhỏ hơn trong cùng năm")
    void testValidateCreateBill_Fail_MonthInPastOrPresent() {
        CreateBillRequest requestInPast = new CreateBillRequest(1L, 4, 2026, new BigDecimal("150.0"), new BigDecimal("60.0"));
        
        when(apartmentService.findById(1L)).thenReturn(Optional.of(apartment));
        when(monthlyMetricService.findFirstByApartmentIdOrderByBillingYearDescBillingMonthDesc(1L))
                .thenReturn(Optional.of(lastMetric));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> billValidationService.validateCreateBill(requestInPast));

        assertTrue(ex.getMessage().contains("Không thể tạo hóa đơn cho khoảng thời gian đã có chỉ số hoặc trong quá khứ"));
    }

    @Test
    @DisplayName("TC-08: Branch - Chỉ số điện mới nhỏ hơn chỉ số cũ")
    void testValidateCreateBill_Fail_ElectricityLessThanOld() {
        CreateBillRequest requestInvalidElec = new CreateBillRequest(1L, 5, 2026, new BigDecimal("99.0"), new BigDecimal("60.0"));
        
        when(apartmentService.findById(1L)).thenReturn(Optional.of(apartment));
        when(monthlyMetricService.findFirstByApartmentIdOrderByBillingYearDescBillingMonthDesc(1L))
                .thenReturn(Optional.of(lastMetric));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> billValidationService.validateCreateBill(requestInvalidElec));

        assertTrue(ex.getMessage().contains("Chỉ số điện mới (99.0) không được nhỏ hơn chỉ số cũ (100.0)"));
    }

    @Test
    @DisplayName("TC-09: Branch - Chỉ số nước mới nhỏ hơn chỉ số cũ")
    void testValidateCreateBill_Fail_WaterLessThanOld() {
        CreateBillRequest requestInvalidWater = new CreateBillRequest(1L, 5, 2026, new BigDecimal("150.0"), new BigDecimal("49.0"));
        
        when(apartmentService.findById(1L)).thenReturn(Optional.of(apartment));
        when(monthlyMetricService.findFirstByApartmentIdOrderByBillingYearDescBillingMonthDesc(1L))
                .thenReturn(Optional.of(lastMetric));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> billValidationService.validateCreateBill(requestInvalidWater));

        assertTrue(ex.getMessage().contains("Chỉ số nước mới (49.0) không được nhỏ hơn chỉ số cũ (50.0)"));
    }

    @Test
    @DisplayName("TC-10: Branch - Căn hộ RENTED nhưng thiếu hợp đồng hoạt động")
    void testCreateRentInvoice_Fail_NoActiveContract() {
        User creator = new User();
        CreateRentInvoiceRequest rentReq = new CreateRentInvoiceRequest(1L, 5, 2026, creator);

        when(residentApartmentService.findActiveTenant(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> rentInvoiceService.createMonthlyRentInvoice(rentReq));

        assertTrue(ex.getMessage().contains("Căn hộ được đánh dấu là ĐANG THUÊ nhưng không tìm thấy hợp đồng thuê nào còn hiệu lực"));
    }

    @Test
    @DisplayName("TC-11: Branch - Hợp đồng hết hạn trước thời điểm hiện tại")
    void testCreateRentInvoice_Fail_ContractExpired() {
        User creator = new User();
        CreateRentInvoiceRequest rentReq = new CreateRentInvoiceRequest(1L, 5, 2026, creator);

        Resident tenant = new Resident();
        tenant.setFullName("Nguyen Van Tenant");

        ResidentApartment contract = new ResidentApartment();
        contract.setResident(tenant);
        contract.setApartment(apartment);
        contract.setContractEnd(LocalDate.now().minusDays(10));

        when(residentApartmentService.findActiveTenant(1L)).thenReturn(Optional.of(contract));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> rentInvoiceService.createMonthlyRentInvoice(rentReq));

        assertTrue(ex.getMessage().contains("đã hết hạn vào ngày"));
    }

    @Test
    @DisplayName("TC-12: Branch - Hợp đồng hết hạn trước kỳ thanh toán")
    void testCreateRentInvoice_Fail_ContractExpiresBeforeBilling() {
        User creator = new User();
        CreateRentInvoiceRequest rentReq = new CreateRentInvoiceRequest(1L, 6, 2026, creator);

        Resident tenant = new Resident();
        tenant.setFullName("Nguyen Van Tenant");

        ResidentApartment contract = new ResidentApartment();
        contract.setResident(tenant);
        contract.setApartment(apartment);
        contract.setContractEnd(LocalDate.of(2026, 6, 15));

        when(residentApartmentService.findActiveTenant(1L)).thenReturn(Optional.of(contract));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> rentInvoiceService.createMonthlyRentInvoice(rentReq));

        assertTrue(ex.getMessage().contains("Hợp đồng sẽ hết hạn trước khi bắt đầu kỳ thanh toán này"));
    }
}
