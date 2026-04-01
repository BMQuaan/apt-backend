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
    public void updateServicePrice(ServicePriceUpdateRequest request) {
        LocalDate today = LocalDate.now();
        LocalDate startOfNextMonth = today.withDayOfMonth(1).plusMonths(1);

        ServiceConfig currentConfig = serviceConfigRepository.findCurrentConfig(request.serviceCode())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dịch vụ: " + request.serviceCode()));

        if (request.effectiveFrom().isBefore(startOfNextMonth)) {
            throw new IllegalArgumentException("Ngày áp dụng cho dịch vụ " + request.serviceCode() +
                    " phải bắt đầu từ " + startOfNextMonth + " trở đi.");
        }

        if (request.newPrice().compareTo(currentConfig.getUnitPrice()) == 0) {
            throw new IllegalArgumentException("Dịch vụ " + request.serviceCode() +
                    " đang được áp dụng mức giá này rồi.");
        }

        Optional<ServiceConfig> upcomingOpt = serviceConfigRepository.findUpcomingConfig(request.serviceCode());

        if (upcomingOpt.isPresent()) {
            ServiceConfig upcoming = upcomingOpt.get();
            upcoming.setUnitPrice(request.newPrice());
            upcoming.setEffectiveFrom(request.effectiveFrom());

            serviceConfigRepository.save(upcoming);
        } else {
            ServiceConfig newConfig = ServiceConfig.builder()
                    .serviceCode(currentConfig.getServiceCode())
                    .serviceName(currentConfig.getServiceName())
                    .unit(currentConfig.getUnit())
                    .unitPrice(request.newPrice())
                    .effectiveFrom(request.effectiveFrom())
                    .build();

            serviceConfigRepository.save(newConfig);
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