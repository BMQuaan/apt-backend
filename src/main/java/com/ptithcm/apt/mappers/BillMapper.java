package com.ptithcm.apt.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ptithcm.apt.dto.response.CreateBillResponse;
import com.ptithcm.apt.dto.response.GetBillDetailByAdminResponse;
import com.ptithcm.apt.dto.response.GetBillsByAdminResponse;
import com.ptithcm.apt.dto.response.GetMyBillDetailByIdResponse;
import com.ptithcm.apt.dto.response.GetMyBillsResponse;
import com.ptithcm.apt.dto.response.UpdateBillStatusResponse;
import com.ptithcm.apt.entity.Bill;

@Mapper(componentModel = "spring")
public interface BillMapper {
    @Mapping(source = "apartment.roomNumber", target = "apartmentName")
    GetBillsByAdminResponse toGetBillsByAdminResponse(Bill bill);

    @Mapping(source = "apartment.id", target = "apartment")
    @Mapping(source = "apartment.roomNumber", target = "apartmentName")
    @Mapping(source = "createdBy.username", target = "createdBy")
    CreateBillResponse toCreateBillResponse(Bill bill);

    @Mapping(source = "apartment.id", target = "apartment")
    @Mapping(source = "apartment.roomNumber", target = "apartmentName")
    @Mapping(source = "createdBy.username", target = "createdBy")
    @Mapping(source = "confirmedBy.username", target = "confirmedBy")
    UpdateBillStatusResponse toUpdateBillStatusResponse(Bill bill);

    @Mapping(source = "apartment.roomNumber", target = "apartmentName")
    GetMyBillsResponse toGetMyBillsResponse(Bill bill);

    @Mapping(source = "apartment.id", target = "apartment")
    @Mapping(source = "apartment.roomNumber", target = "apartmentName")
    @Mapping(source = "confirmedBy.username", target = "confirmBy")
    GetMyBillDetailByIdResponse toGetMyBillDetailByIdResponse(Bill bill);

    @Mapping(source = "apartment.id", target = "apartmentId")
    @Mapping(source = "apartment.roomNumber", target = "apartmentName")
    @Mapping(source = "apartment.floor", target = "apartmentFloor")
    @Mapping(source = "apartment.area", target = "apartmentArea")
    @Mapping(source = "createdBy.username", target = "createdBy")
    @Mapping(source = "confirmedBy.username", target = "confirmBy")
    GetBillDetailByAdminResponse toGetBillDetailByAdminResponse(Bill bill);
}
