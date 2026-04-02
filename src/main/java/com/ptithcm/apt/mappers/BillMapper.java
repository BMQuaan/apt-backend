package com.ptithcm.apt.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ptithcm.apt.dto.response.GetBillsByAdminResponse;
import com.ptithcm.apt.entity.Bill;

@Mapper(componentModel = "spring")
public interface BillMapper {
    @Mapping(source = "apartment.roomNumber", target = "apartmentName")
    GetBillsByAdminResponse toGetBillsByAdminResponse(Bill bill);
}
 