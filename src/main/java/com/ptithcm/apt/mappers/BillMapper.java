package com.ptithcm.apt.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ptithcm.apt.dto.response.BillResponse;
import com.ptithcm.apt.dto.response.AdminBillDetailResponse;
import com.ptithcm.apt.dto.response.AdminBillListResponse;
import com.ptithcm.apt.dto.response.UserBillDetailReponse;
import com.ptithcm.apt.dto.response.UserBillListResponse;
import com.ptithcm.apt.dto.response.UpdateBillStatusResponse;
import com.ptithcm.apt.entity.Bill;

@Mapper(componentModel = "spring")
public interface BillMapper {
    @Mapping(source = "apartment.roomNumber", target = "apartmentName")
    AdminBillListResponse toGetBillsByAdminResponse(Bill bill);

    @Mapping(source = "apartment.id", target = "apartment")
    @Mapping(source = "apartment.roomNumber", target = "apartmentName")
    @Mapping(source = "createdBy.username", target = "createdBy")
    BillResponse toCreateBillResponse(Bill bill);

    @Mapping(source = "apartment.id", target = "apartment")
    @Mapping(source = "apartment.roomNumber", target = "apartmentName")
    @Mapping(source = "createdBy.username", target = "createdBy")
    @Mapping(source = "confirmedBy.username", target = "confirmedBy")
    UpdateBillStatusResponse toUpdateBillStatusResponse(Bill bill);

    @Mapping(source = "apartment.roomNumber", target = "apartmentName")
    UserBillListResponse toGetMyBillsResponse(Bill bill);

    @Mapping(source = "apartment.id", target = "apartment")
    @Mapping(source = "apartment.roomNumber", target = "apartmentName")
    @Mapping(source = "confirmedBy.username", target = "confirmBy")
    UserBillDetailReponse toGetMyBillDetailByIdResponse(Bill bill);

    @Mapping(source = "apartment.id", target = "apartmentId")
    @Mapping(source = "apartment.roomNumber", target = "apartmentName")
    @Mapping(source = "apartment.floor", target = "apartmentFloor")
    @Mapping(source = "apartment.area", target = "apartmentArea")
    @Mapping(source = "createdBy.username", target = "createdBy")
    @Mapping(source = "confirmedBy.username", target = "confirmBy")
    AdminBillDetailResponse toGetBillDetailByAdminResponse(Bill bill);
}
