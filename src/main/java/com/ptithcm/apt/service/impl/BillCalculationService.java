package com.ptithcm.apt.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ptithcm.apt.dto.BillFeeResult;
import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.entity.ServiceConfig;
import com.ptithcm.apt.service.ServiceConfigService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BillCalculationService {

        private final ServiceConfigService serviceConfigService;

        /**
         * Tính phí dịch vụ dựa trên config giá và chỉ số điện/nước.
         *
         * @param apt      căn hộ cần tính phí
         * @param newElec  chỉ số điện mới
         * @param oldElec  chỉ số điện cũ
         * @param newWater chỉ số nước mới
         * @param oldWater chỉ số nước cũ
         * @return kết quả tính phí gồm từng loại phí và tổng
         */
        public BillFeeResult calculateFees(Apartment apt,
                        BigDecimal newElec, BigDecimal oldElec,
                        BigDecimal newWater, BigDecimal oldWater) {
                List<ServiceConfig> configs = serviceConfigService.findAllCurrentConfigs();
                Map<String, BigDecimal> priceMap = configs.stream()
                                .collect(Collectors.toMap(ServiceConfig::getServiceCode, ServiceConfig::getUnitPrice));

                BigDecimal waterFee = priceMap.get("WATER")
                                .multiply(BigDecimal.valueOf(newWater.longValue()).subtract(oldWater));
                BigDecimal electricityFee = priceMap.get("ELECTRICITY")
                                .multiply(BigDecimal.valueOf(newElec.longValue()).subtract(oldElec));
                BigDecimal managementFee = priceMap.get("MANAGEMENT").multiply(apt.getArea());
                BigDecimal sanitationFee = priceMap.get("SANITATION");

                BigDecimal totalAmount = waterFee
                                .add(managementFee)
                                .add(sanitationFee)
                                .add(electricityFee);

                return new BillFeeResult(waterFee, electricityFee, managementFee, sanitationFee, totalAmount);
        }
}
