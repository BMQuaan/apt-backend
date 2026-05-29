package com.ptithcm.apt.usecases.uc05_service_config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ptithcm.apt.dto.request.ServicePriceUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.ptithcm.apt.entity.ServiceConfig;
import com.ptithcm.apt.repository.ServiceConfigRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("UC05_ServiceConfig_IntegrationTest - Kiểm thử Tích hợp (Phân quyền API & Transaction)")
public class UC05_ServiceConfig_IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServiceConfigRepository serviceConfigRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Module này cho phép Jackson map được kiểu LocalDate của Java 8
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @WithMockUser(username = "resident@gmail.com", roles = "USER")
    @DisplayName("TC-01: User thông thường cố tình gọi API Cập nhật giá -> Bị chặn (HTTP 403 Forbidden)")
    void testUpdatePrice_ForbiddenForNormalUser() throws Exception {
        ServicePriceUpdateRequest request = new ServicePriceUpdateRequest("WATER", new BigDecimal("20000"),
                LocalDate.now().plusMonths(1));

        mockMvc.perform(post("/api/v1/service-configs/upcoming")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                // Kiểm chứng: Spring Security sẽ nhận diện role USER và tát văng ra mã lỗi 403
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bạn không có quyền truy cập tài nguyên này."));
    }

    @Test
    @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
    @DisplayName("TC-02: Admin gọi API nhưng truyền sai định dạng -> Qua được chốt 403, bị chặn ở 400")
    void testUpdatePrice_AdminPassesSecurityButFailsValidation() throws Exception {
        // Cố tình truyền một cái Request sai định dạng (Giá bằng 0)
        ServicePriceUpdateRequest request = new ServicePriceUpdateRequest("WATER", BigDecimal.ZERO,
                LocalDate.now().plusMonths(1));

        mockMvc.perform(post("/api/v1/service-configs/upcoming")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                // Kiểm chứng: Nhờ mang thẻ ADMIN, người dùng đã vượt qua được lớp Security.
                // Nhưng ngay sau đó, Annotation @Valid ở Controller sẽ quăng lỗi 400 Bad
                // Request.
                // Điều này chứng minh chức năng phân quyền đã hoạt động hoàn hảo!
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
    @DisplayName("TC-07 (Integration): Đã có bản ghi Upcoming -> Cập nhật bản ghi đó thay vì tạo mới")
    void testUpdatePrice_UpdateExistingUpcoming_Transaction() throws Exception {
        // 1. Chuẩn bị dữ liệu: Tạo nhiều bản ghi quá khứ và 1 bản ghi chờ
        String serviceCode = "MANAGEMENT";
        long initialCount = serviceConfigRepository.findAll().stream()
                .filter(s -> s.getServiceCode().equals(serviceCode)).count();

        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusMonths(1).withDayOfMonth(1);

        ServiceConfig oldConfig = new ServiceConfig();
        oldConfig.setServiceCode(serviceCode);
        oldConfig.setServiceName("Phí quản lý chung cư");
        oldConfig.setUnitPrice(new BigDecimal("40000"));
        oldConfig.setUnit("VND/m2");
        oldConfig.setEffectiveFrom(today.minusYears(1));
        serviceConfigRepository.save(oldConfig);

        ServiceConfig currentConfig = new ServiceConfig();
        currentConfig.setServiceCode(serviceCode);
        currentConfig.setServiceName("Phí quản lý chung cư");
        currentConfig.setUnitPrice(new BigDecimal("50000"));
        currentConfig.setUnit("VND/m2");
        currentConfig.setEffectiveFrom(today.minusMonths(1));
        serviceConfigRepository.save(currentConfig);

        ServiceConfig upcomingConfig = new ServiceConfig();
        upcomingConfig.setServiceCode(serviceCode);
        upcomingConfig.setServiceName("Phí quản lý chung cư");
        upcomingConfig.setUnitPrice(new BigDecimal("60000")); // Giá chờ hiện tại là 60k
        upcomingConfig.setUnit("VND/m2");
        upcomingConfig.setEffectiveFrom(nextMonth);
        serviceConfigRepository.save(upcomingConfig);

        // 2. Gọi API để cập nhật giá chờ thành 70k
        ServicePriceUpdateRequest request = new ServicePriceUpdateRequest(serviceCode, new BigDecimal("70000"),
                nextMonth);

        mockMvc.perform(post("/api/v1/service-configs/upcoming")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 3. Kiểm chứng trong Database (Transaction không tạo thêm dòng mới, chỉ
        // update)
        Optional<ServiceConfig> updatedUpcomingOpt = serviceConfigRepository.findUpcomingConfig(serviceCode, today);
        assertTrue(updatedUpcomingOpt.isPresent(), "Bản ghi upcoming phải tồn tại");
        assertEquals(new BigDecimal("70000.00"), updatedUpcomingOpt.get().getUnitPrice().setScale(2),
                "Giá phải được cập nhật thành 70000");

        // Đảm bảo không bị insert thêm (tổng số bản ghi cho service này là initialCount
        // + 3)
        // (3 bản ghi do test này tạo ra: old, current và upcoming, và API chỉ update
        // upcoming)
        long count = serviceConfigRepository.findAll().stream().filter(s -> s.getServiceCode().equals(serviceCode))
                .count();
        assertEquals(initialCount + 3, count, "Chỉ được phép update, không tạo thêm bản ghi nào khác");
    }

    @Test
    @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
    @DisplayName("TC-08 (Integration): Chưa có bản ghi Upcoming -> Tạo mới bản ghi chờ")
    void testUpdatePrice_CreateNewUpcoming_Transaction() throws Exception {
        // 1. Chuẩn bị dữ liệu: Tạo nhiều bản ghi quá khứ (không có bản ghi chờ)
        String serviceCode = "WATER";
        long initialCount = serviceConfigRepository.findAll().stream()
                .filter(s -> s.getServiceCode().equals(serviceCode)).count();

        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusMonths(1).withDayOfMonth(1);

        ServiceConfig oldConfig = new ServiceConfig();
        oldConfig.setServiceCode(serviceCode);
        oldConfig.setServiceName("Tiền nước sinh hoạt");
        oldConfig.setUnitPrice(new BigDecimal("150000"));
        oldConfig.setUnit("VND/m3");
        oldConfig.setEffectiveFrom(today.minusYears(1));
        serviceConfigRepository.save(oldConfig);

        ServiceConfig currentConfig = new ServiceConfig();
        currentConfig.setServiceCode(serviceCode);
        currentConfig.setServiceName("Tiền nước sinh hoạt");
        currentConfig.setUnitPrice(new BigDecimal("200000"));
        currentConfig.setUnit("VND/m3");
        currentConfig.setEffectiveFrom(today.minusMonths(1));
        serviceConfigRepository.save(currentConfig);

        // Đảm bảo chưa có bản ghi upcoming nếu lỗi thì do db đã có bản ghi upcoming
        Optional<ServiceConfig> noUpcoming = serviceConfigRepository.findUpcomingConfig(serviceCode, today);
        assertTrue(noUpcoming.isEmpty());

        // 2. Gọi API để thiết lập giá mới từ tháng sau
        ServicePriceUpdateRequest request = new ServicePriceUpdateRequest(serviceCode, new BigDecimal("250000"),
                nextMonth);

        mockMvc.perform(post("/api/v1/service-configs/upcoming")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 3. Kiểm chứng trong Database (Transaction đã insert thêm 1 dòng mới)
        Optional<ServiceConfig> newUpcomingOpt = serviceConfigRepository.findUpcomingConfig(serviceCode, today);
        assertTrue(newUpcomingOpt.isPresent(), "Bản ghi upcoming mới phải được tạo");
        assertEquals(new BigDecimal("250000.00"), newUpcomingOpt.get().getUnitPrice().setScale(2),
                "Giá mới phải là 250000");

        // Tổng số bản ghi lúc này là initialCount + 3 (2 bản quá khứ test tạo + 1 bản
        // tương lai API tạo)
        long count = serviceConfigRepository.findAll().stream().filter(s -> s.getServiceCode().equals(serviceCode))
                .count();
        assertEquals(initialCount + 3, count, "Phải insert thêm đúng 1 bản ghi tương lai");
    }
}
