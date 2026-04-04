package com.ptithcm.apt.repository;

import com.ptithcm.apt.dto.response.ServiceConfigResponse;
import com.ptithcm.apt.entity.ServiceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ServiceConfigRepository extends JpaRepository<ServiceConfig, Long> {

    // Tìm giá đang áp dụng (<= ngày hiện tại)
    @Query(value = """
        SELECT * FROM service_configs 
        WHERE service_code = :serviceCode AND effective_from <= :currentDate 
        ORDER BY effective_from DESC, updated_at DESC LIMIT 1
    """, nativeQuery = true)
    Optional<ServiceConfig> findCurrentConfig(@Param("serviceCode") String serviceCode, @Param("currentDate") LocalDate currentDate);

    // Tìm giá chờ cập nhật trong tương lai (> ngày hiện tại)
    @Query(value = """
        SELECT * FROM service_configs 
        WHERE service_code = :serviceCode AND effective_from > :currentDate 
        ORDER BY effective_from ASC LIMIT 1
    """, nativeQuery = true)
    Optional<ServiceConfig> findUpcomingConfig(@Param("serviceCode") String serviceCode, @Param("currentDate") LocalDate currentDate);

    @Query(value = """
        SELECT DISTINCT ON (service_code) * FROM service_configs 
        WHERE effective_from <= :targetDate 
        ORDER BY service_code, effective_from DESC, updated_at DESC
    """, nativeQuery = true)
    List<ServiceConfig> findAllConfigsActiveOnDate(@Param("targetDate") LocalDate targetDate);

    @Query("SELECT DISTINCT s.serviceCode FROM ServiceConfig s")
    List<String> findDistinctServiceCodes();
}
