package com.ptithcm.apt.usecases.uc05_service_config;

import com.ptithcm.apt.dto.request.ServicePriceUpdateRequest;
import com.ptithcm.apt.entity.ServiceConfig;
import com.ptithcm.apt.exception.NotFoundException;
import com.ptithcm.apt.mapper.ServiceConfigMapper;
import com.ptithcm.apt.repository.ServiceConfigRepository;
import com.ptithcm.apt.service.impl.ServiceConfigServiceImpl;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UC05_ServiceConfig_UnitTest - Kiểm thử Đơn vị (DTO & Service)")
public class UC05_ServiceConfig_UnitTest {

    @Nested
    @DisplayName("1. Kiểm thử Validation Đầu vào (DTO)")
    class DTOValidationTest {

        private static Validator validator;

        @BeforeAll
        static void setUpValidator() {
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        }

        @Test
        @DisplayName("TC-03: BVA - Giá mới <= 0")
        void testUpdateRequest_InvalidPrice() {
            // Giá bằng 0
            ServicePriceUpdateRequest requestZero = new ServicePriceUpdateRequest("WATER", BigDecimal.ZERO, LocalDate.now().plusMonths(1));
            Set<ConstraintViolation<ServicePriceUpdateRequest>> violationsZero = validator.validate(requestZero);
            assertFalse(violationsZero.isEmpty());
            assertTrue(violationsZero.stream().anyMatch(v -> v.getMessage().contains("Giá dịch vụ phải lớn 0")));

            // Giá âm
            ServicePriceUpdateRequest requestNegative = new ServicePriceUpdateRequest("WATER", new BigDecimal("-100"), LocalDate.now().plusMonths(1));
            Set<ConstraintViolation<ServicePriceUpdateRequest>> violationsNegative = validator.validate(requestNegative);
            assertFalse(violationsNegative.isEmpty());
        }

        @ParameterizedTest(name = "Validation: {0} = {1}")
        @CsvSource({
                "serviceCode, '', Mã dịch vụ không được để trống"
        })
        @DisplayName("Kiểm tra Validation bị rỗng")
        void testUpdateRequest_BlankFields(String field, String value, String expectedMessage) {
            ServicePriceUpdateRequest request = new ServicePriceUpdateRequest(value, new BigDecimal("10000"), LocalDate.now().plusMonths(1));
            Set<ConstraintViolation<ServicePriceUpdateRequest>> violations = validator.validate(request);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals(expectedMessage)));
        }
    }

    @Nested
    @DisplayName("2. Kiểm thử Logic Nghiệp vụ (ServiceConfigServiceImpl)")
    class ServiceLogicTest {

        @Mock
        private ServiceConfigRepository serviceConfigRepository;
        @Mock
        private ServiceConfigMapper serviceConfigMapper;

        @InjectMocks
        private ServiceConfigServiceImpl serviceConfigService;

        private ServiceConfig currentConfig;
        private LocalDate today;
        private LocalDate startOfNextMonth;

        @BeforeEach
        void setUp() {
            today = LocalDate.now();
            startOfNextMonth = today.withDayOfMonth(1).plusMonths(1);

            currentConfig = new ServiceConfig();
            currentConfig.setServiceCode("ELECTRICITY");
            currentConfig.setServiceName("Điện");
            currentConfig.setUnitPrice(new BigDecimal("3500"));
            currentConfig.setUnit("kWh");
            currentConfig.setEffectiveFrom(today.minusMonths(1));
        }

        @Test
        @DisplayName("TC-04: Nhánh 1 - Mã dịch vụ không tồn tại")
        void testUpdateServicePrice_ServiceNotFound() {
            ServicePriceUpdateRequest request = new ServicePriceUpdateRequest("DIEN_KHONG_CO", new BigDecimal("4000"), startOfNextMonth);
            
            // Giả lập DB trả về rỗng khi tìm mã dịch vụ
            when(serviceConfigRepository.findCurrentConfig(eq("DIEN_KHONG_CO"), any(LocalDate.class)))
                    .thenReturn(Optional.empty());

            NotFoundException exception = assertThrows(NotFoundException.class, () -> serviceConfigService.updateServicePrice(request));
            assertTrue(exception.getMessage().contains("Không tìm thấy dịch vụ"));
        }

        @Test
        @DisplayName("TC-05: Nhánh 2 - Áp dụng giá mới vào THÁNG HIỆN TẠI (Lỗi)")
        void testUpdateServicePrice_EffectiveDateInCurrentMonth() {
            // Cố tình truyền vào ngày của tháng hiện tại
            ServicePriceUpdateRequest request = new ServicePriceUpdateRequest("ELECTRICITY", new BigDecimal("4000"), today);
            
            when(serviceConfigRepository.findCurrentConfig(eq("ELECTRICITY"), any(LocalDate.class)))
                    .thenReturn(Optional.of(currentConfig));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> serviceConfigService.updateServicePrice(request));
            assertTrue(exception.getMessage().contains("phải bắt đầu từ tháng"));
            // Xác nhận DB không được gọi save()
            verify(serviceConfigRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-06: Nhánh 3 - Giá mới bị trùng với giá hiện tại")
        void testUpdateServicePrice_PriceNotChanged() {
            // Truyền giá cũ (3500)
            ServicePriceUpdateRequest request = new ServicePriceUpdateRequest("ELECTRICITY", new BigDecimal("3500"), startOfNextMonth);
            
            when(serviceConfigRepository.findCurrentConfig(eq("ELECTRICITY"), any(LocalDate.class)))
                    .thenReturn(Optional.of(currentConfig));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> serviceConfigService.updateServicePrice(request));
            assertTrue(exception.getMessage().contains("đang được áp dụng mức giá này rồi"));
        }

        @Test
        @DisplayName("TC-07: Nhánh 4 - ĐÃ CÓ sẵn bản ghi chờ (Upcoming). Cập nhật đè lên!")
        void testUpdateServicePrice_UpdateExistingUpcomingConfig() {
            ServicePriceUpdateRequest request = new ServicePriceUpdateRequest("ELECTRICITY", new BigDecimal("4000"), startOfNextMonth);
            
            when(serviceConfigRepository.findCurrentConfig(eq("ELECTRICITY"), any(LocalDate.class)))
                    .thenReturn(Optional.of(currentConfig));

            // Giả lập ĐÃ CÓ 1 bản ghi Upcoming trong DB (Ví dụ trước đó lỡ đặt giá 3800)
            ServiceConfig upcomingConfig = new ServiceConfig();
            upcomingConfig.setId(99L);
            upcomingConfig.setUnitPrice(new BigDecimal("3800"));
            
            when(serviceConfigRepository.findUpcomingConfig(eq("ELECTRICITY"), any(LocalDate.class)))
                    .thenReturn(Optional.of(upcomingConfig));

            serviceConfigService.updateServicePrice(request);

            // Xác minh: Code lấy bản ghi ID 99 đó và thay giá thành 4000, sau đó lưu lại
            assertEquals(new BigDecimal("4000"), upcomingConfig.getUnitPrice());
            verify(serviceConfigRepository, times(1)).save(upcomingConfig);
        }

        @Test
        @DisplayName("TC-08: Nhánh 4 - CHƯA CÓ bản ghi chờ. Sinh ra bản ghi mới hoàn toàn!")
        void testUpdateServicePrice_CreateNewUpcomingConfig() {
            ServicePriceUpdateRequest request = new ServicePriceUpdateRequest("WATER", new BigDecimal("25000"), startOfNextMonth);
            
            currentConfig.setServiceCode("WATER");
            currentConfig.setUnitPrice(new BigDecimal("20000"));

            when(serviceConfigRepository.findCurrentConfig(eq("WATER"), any(LocalDate.class)))
                    .thenReturn(Optional.of(currentConfig));

            // Giả lập CHƯA CÓ bản ghi Upcoming nào
            when(serviceConfigRepository.findUpcomingConfig(eq("WATER"), any(LocalDate.class)))
                    .thenReturn(Optional.empty());

            serviceConfigService.updateServicePrice(request);

            // Bắt lấy Object mà code truyền vào hàm save() để kiểm tra
            ArgumentCaptor<ServiceConfig> captor = ArgumentCaptor.forClass(ServiceConfig.class);
            verify(serviceConfigRepository, times(1)).save(captor.capture());
            
            ServiceConfig savedConfig = captor.getValue();
            // Đảm bảo tạo ra một bản ghi mới có chứa mã, giá mới và ngày áp dụng mới
            assertEquals("WATER", savedConfig.getServiceCode());
            assertEquals(new BigDecimal("25000"), savedConfig.getUnitPrice());
            assertEquals(startOfNextMonth, savedConfig.getEffectiveFrom());
        }
    }
}
