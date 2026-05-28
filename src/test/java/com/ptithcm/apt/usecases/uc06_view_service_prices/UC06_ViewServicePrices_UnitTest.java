package com.ptithcm.apt.usecases.uc06_view_service_prices;

import com.ptithcm.apt.dto.response.ServiceConfigResponse;
import com.ptithcm.apt.entity.ServiceConfig;
import com.ptithcm.apt.mapper.ServiceConfigMapper;
import com.ptithcm.apt.repository.ServiceConfigRepository;
import com.ptithcm.apt.service.impl.ServiceConfigServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UC06_ViewServicePrices_UnitTest - Kiểm thử Đơn vị (Service Logic)")
public class UC06_ViewServicePrices_UnitTest {

    @Mock
    private ServiceConfigRepository serviceConfigRepository;

    @Mock
    private ServiceConfigMapper serviceConfigMapper;

    @InjectMocks
    private ServiceConfigServiceImpl serviceConfigService;

    @Test
    @DisplayName("TC-01 & TC-02: Truy vấn ngày CÓ dữ liệu -> Trả về danh sách")
    void testGetPricesByDate_HasData() {
        LocalDate targetDate = LocalDate.of(2024, 12, 1);
        
        // Giả lập DB trả về 2 bản ghi Điện và Nước
        ServiceConfig config1 = new ServiceConfig();
        config1.setServiceCode("ELECTRICITY");
        config1.setUnitPrice(new BigDecimal("3500"));

        ServiceConfig config2 = new ServiceConfig();
        config2.setServiceCode("WATER");
        config2.setUnitPrice(new BigDecimal("20000"));

        List<ServiceConfig> mockEntities = Arrays.asList(config1, config2);
        
        // Giả lập Mapper trả về DTO
        ServiceConfigResponse res1 = ServiceConfigResponse.builder().serviceCode("ELECTRICITY").unitPrice(new BigDecimal("3500")).build();
        ServiceConfigResponse res2 = ServiceConfigResponse.builder().serviceCode("WATER").unitPrice(new BigDecimal("20000")).build();
        List<ServiceConfigResponse> mockResponses = Arrays.asList(res1, res2);

        when(serviceConfigRepository.findAllConfigsActiveOnDate(targetDate)).thenReturn(mockEntities);
        when(serviceConfigMapper.toResponseList(mockEntities)).thenReturn(mockResponses);

        List<ServiceConfigResponse> result = serviceConfigService.getPricesByDate(targetDate);

        assertEquals(2, result.size(), "Phải trả về đúng 2 bản ghi");
        assertEquals("ELECTRICITY", result.get(0).serviceCode());
    }

    @Test
    @DisplayName("TC-05: Truy vấn ngày KHÔNG CÓ dữ liệu (Quá khứ xa) -> Trả về mảng rỗng []")
    void testGetPricesByDate_EmptyData() {
        LocalDate oldDate = LocalDate.of(1990, 1, 1);
        
        // Giả lập DB trả về rỗng
        when(serviceConfigRepository.findAllConfigsActiveOnDate(oldDate)).thenReturn(Collections.emptyList());
        when(serviceConfigMapper.toResponseList(Collections.emptyList())).thenReturn(Collections.emptyList());

        List<ServiceConfigResponse> result = serviceConfigService.getPricesByDate(oldDate);

        // Đảm bảo không bị crash mà chỉ trả về List rỗng
        assertTrue(result.isEmpty(), "Danh sách trả về phải là mảng rỗng []");
    }
}
