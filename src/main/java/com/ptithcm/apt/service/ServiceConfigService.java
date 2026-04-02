package com.ptithcm.apt.service;

import com.ptithcm.apt.dto.request.ServicePriceUpdateRequest;
import com.ptithcm.apt.dto.response.AdminServiceConfigResponse;
import com.ptithcm.apt.dto.response.ServiceConfigResponse;

import java.time.LocalDate;
import java.util.List;

public interface ServiceConfigService {
    public void updateServicePrice(ServicePriceUpdateRequest request);
    public List<AdminServiceConfigResponse> getAdminDashboardPrices();
    public void cancelUpcomingUpdate(String serviceCode);
    public List<ServiceConfigResponse> getPricesByDate(LocalDate targetDate);
}
