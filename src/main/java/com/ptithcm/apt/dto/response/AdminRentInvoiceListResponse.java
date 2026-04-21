package com.ptithcm.apt.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminRentInvoiceListResponse(
                Long id,
                String apartmentName,
                Integer billingMonth,
                Integer billingYear,
                BigDecimal rentAmount,
                String status,
                LocalDateTime dueDate) {

}
