package com.ptithcm.apt.service;

import com.ptithcm.apt.entity.User;

public interface RentInvoiceService {
    void createMonthlyRentInvoice(Long apartmentId, Integer month, Integer year, User creator);

}
