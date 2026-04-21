package com.ptithcm.apt.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ptithcm.apt.dto.response.AdminRentInvoiceListResponse;
import com.ptithcm.apt.dto.response.RentInvoiceResponse;
import com.ptithcm.apt.entity.RentInvoice;

@Mapper(componentModel = "spring")
public interface RentInvoiceMapper {
    @Mapping(source = "apartment.id", target = "apartment")
    @Mapping(source = "apartment.roomNumber", target = "apartmentName")
    @Mapping(source = "tenant.fullName", target = "tenantName")
    @Mapping(source = "owner.fullName", target = "ownerName")
    @Mapping(source = "createdBy.username", target = "createdBy")
    RentInvoiceResponse toCreateRentInvoiceResponse(RentInvoice rentInvoice);

    @Mapping(source = "apartment.roomNumber", target = "apartmentName")
    AdminRentInvoiceListResponse toGetRentInvoiceListResponse(RentInvoice rentInvoice);
}
