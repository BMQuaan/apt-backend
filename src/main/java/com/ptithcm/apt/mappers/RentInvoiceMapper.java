package com.ptithcm.apt.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ptithcm.apt.dto.response.CreateRentInvoiceResponse;
import com.ptithcm.apt.entity.RentInvoice;

@Mapper(componentModel = "spring")
public interface RentInvoiceMapper {
    @Mapping(source = "apartment.id", target = "apartment")
    @Mapping(source = "apartment.roomNumber", target = "apartmentName")
    @Mapping(source = "tenant.fullName", target = "tenantName")
    @Mapping(source = "owner.fullName", target = "ownerName")
    @Mapping(source = "createdBy.username", target = "createdBy")
    CreateRentInvoiceResponse toCreateRentInvoiceResponse(RentInvoice rentInvoice);
}
