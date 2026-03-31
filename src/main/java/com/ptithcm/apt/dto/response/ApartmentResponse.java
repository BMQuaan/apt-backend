package com.ptithcm.apt.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApartmentResponse {
    // private Long id;
    private String roomNumber;
    private Integer floor;
    private BigDecimal area;
    private String status;
    private LocalDateTime createdAt;
}
