package com.ptithcm.apt.dto.request;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApartmentRequest {
    private String roomNumber;
    private Integer floor;
    private BigDecimal area;
    private String status;
}