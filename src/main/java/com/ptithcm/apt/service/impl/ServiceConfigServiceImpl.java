package com.ptithcm.apt.service.impl;

import com.ptithcm.apt.dto.request.ServicePriceUpdateRequest;
import com.ptithcm.apt.dto.response.AdminServiceConfigResponse;
import com.ptithcm.apt.dto.response.ServiceConfigResponse;
import com.ptithcm.apt.entity.ServiceConfig;
import com.ptithcm.apt.exception.NotFoundException;
import com.ptithcm.apt.mapper.ServiceConfigMapper;
import com.ptithcm.apt.repository.ServiceConfigRepository;
import com.ptithcm.apt.service.ServiceConfigService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServiceConfigServiceImpl implements ServiceConfigService {

    private final ServiceConfigMapper serviceConfigMapper;
    private final ServiceConfigRepository serviceConfigRepository;


    @Override
    @Transactional
    public void updateServicePrices(ServicePriceUpdateRequest request) {
        LocalDate today = LocalDate.now();
        LocalDate startOfNextMonth = today.withDayOfMonth(1).plusMonths(1);

        List<ServiceConfig> configsToSave = new ArrayList<>();

        for (ServicePriceUpdateRequest.ServicePrice priceReq : request.prices()) {

            ServiceConfig currentConfig = serviceConfigRepository.findCurrentConfig(priceReq.serviceCode())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy dịch vụ: " + priceReq.serviceCode()));

            // Chỉ cho phép áp dụng từ "ít nhất tháng sau"
            if (priceReq.effectiveFrom().isBefore(startOfNextMonth)) {
                throw new IllegalArgumentException("Ngày áp dụng cho dịch vụ " + priceReq.serviceCode() +
                        " phải bắt đầu từ " + startOfNextMonth + " trở đi.");
            }

            // Không cho update bằng giá hiện tại
            if (priceReq.newPrice().compareTo(currentConfig.getUnitPrice()) == 0) {
                    throw new IllegalArgumentException("Dịch vụ " + priceReq.serviceCode() +
                            " đang được áp dụng mức giá này rồi.");
            }

            // XỬ LÝ UPDATE CHO TƯƠNG LAI
            Optional<ServiceConfig> upcomingOpt = serviceConfigRepository.findUpcomingConfig(priceReq.serviceCode());

            if (upcomingOpt.isPresent()) {
                // Đã có 1 bản ghi chờ -> Cập nhật đè lên nó
                ServiceConfig upcoming = upcomingOpt.get();
                upcoming.setUnitPrice(priceReq.newPrice());
                upcoming.setEffectiveFrom(priceReq.effectiveFrom());
                configsToSave.add(upcoming);
            } else {
                // Chưa có bản ghi chờ -> Tạo 1 bản ghi tương lai mới
                ServiceConfig newConfig = ServiceConfig.builder()
                        .serviceCode(currentConfig.getServiceCode())
                        .serviceName(currentConfig.getServiceName())
                        .unit(currentConfig.getUnit())
                        .unitPrice(priceReq.newPrice())
                        .effectiveFrom(priceReq.effectiveFrom())
                        .build();
                configsToSave.add(newConfig);
            }
        }

        if (!configsToSave.isEmpty()) {
            serviceConfigRepository.saveAll(configsToSave);
        }
    }

    @Override
    public List<AdminServiceConfigResponse> getAdminDashboardPrices() {
        List<String> serviceCodes = List.of("ELECTRICITY", "WATER", "MANAGEMENT", "SANITATION");
        List<AdminServiceConfigResponse> responses = new ArrayList<>();

        for (String code : serviceCodes) {
            ServiceConfig current = serviceConfigRepository.findCurrentConfig(code).orElse(null);
            ServiceConfig upcoming = serviceConfigRepository.findUpcomingConfig(code).orElse(null);

            if (current != null) {
                responses.add(AdminServiceConfigResponse.builder()
                        .serviceCode(current.getServiceCode())
                        .serviceName(current.getServiceName())
                        .unit(current.getUnit())
                        .currentPrice(current.getUnitPrice())
                        .currentEffectiveFrom(current.getEffectiveFrom())
                        .upcomingPrice(upcoming != null ? upcoming.getUnitPrice() : null)
                        .upcomingEffectiveFrom(upcoming != null ? upcoming.getEffectiveFrom() : null)
                        .build());
            }
        }
        return responses;
    }

    @Override
    @Transactional
    public void cancelUpcomingUpdate(String serviceCode) {
        ServiceConfig upcomingConfig = serviceConfigRepository.findUpcomingConfig(serviceCode)
                .orElseThrow(() -> new NotFoundException("Không có bản ghi chờ cập nhật nào cho dịch vụ: " + serviceCode));

        serviceConfigRepository.delete(upcomingConfig);
    }

    @Override
    public List<ServiceConfigResponse> getPricesByDate(LocalDate targetDate) {
        List<ServiceConfig> activeConfigs = serviceConfigRepository.findAllConfigsActiveOnDate(targetDate);

        return serviceConfigMapper.toResponseList(activeConfigs);
    }
}