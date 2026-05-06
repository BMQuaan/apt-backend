package com.ptithcm.apt.service.scheduler;

import com.ptithcm.apt.entity.Bill;
import com.ptithcm.apt.enums.BillStatus;
import com.ptithcm.apt.repository.BillRepository;
import com.ptithcm.apt.repository.ResidentApartmentRepository;
import com.ptithcm.apt.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j // Dùng để xem log trong Console
public class BillStatusScheduler {

    private final BillRepository billRepository;
    private final ResidentApartmentRepository residentApartmentRepository; // Thêm repo này
    private final EmailService emailService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationStart() {
        log.info(">>> System started. Executing initial overdue bills check...");
        autoUpdateLateStatus();
    }

    /**
     * Chạy mỗi ngày vào lúc 00:00:00
     * Cron format: giây phút giờ ngày tháng thứ
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void autoUpdateLateStatus() {
        log.info("Cron Job: Checking for overdue bills at {}", LocalDateTime.now());

        List<Bill> overdueBills = billRepository.findAllByStatusAndDueDateBefore(
                BillStatus.UNPAID,
                LocalDateTime.now());

        if (!overdueBills.isEmpty()) {
            overdueBills.forEach(bill -> {
                bill.setStatus(BillStatus.LATE);
                sendOverdueEmail(bill);
                log.info("Bill #{} (Apt: {}) is overdue. Status changed to LATE.",
                        bill.getId(), bill.getApartment().getRoomNumber());
            });

            billRepository.saveAll(overdueBills);
            log.info("Successfully updated {} bills to LATE.", overdueBills.size());
        } else {
            log.info("No overdue bills found today.");
        }
    }

    private void sendOverdueEmail(Bill bill) {
        residentApartmentRepository.findByApartmentIdAndIsHeadTrueAndIsActiveTrue(bill.getApartment().getId())
                .ifPresent(headResident -> {
                    if (headResident.getResident().getEmail() != null) {
                        try {
                            java.util.Map<String, String> templateModel = java.util.Map.of(
                                    "fullName", headResident.getResident().getFullName(),
                                    "roomNumber", bill.getApartment().getRoomNumber(),
                                    "month", String.valueOf(bill.getBillingMonth()),
                                    "year", String.valueOf(bill.getBillingYear()),
                                    "electricityFee", String.format("%,.0f", bill.getElectricityFee()),
                                    "waterFee", String.format("%,.0f", bill.getWaterFee()),
                                    "managementFee", String.format("%,.0f", bill.getManagementFee()),
                                    "sanitationFee", String.format("%,.0f", bill.getSanitationFee()),
                                    "totalAmount", String.format("%,.0f", bill.getTotalAmount()),
                                    "dueDate", bill.getDueDate()
                                            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));

                            emailService.sendHtmlEmail(
                                    headResident.getResident().getEmail(),
                                    "[AptApp] CẢNH BÁO QUÁ HẠN THANH TOÁN PHÍ DỊCH VỤ - Căn hộ "
                                            + bill.getApartment().getRoomNumber(),
                                    "overdue_bill_template_vi.html",
                                    templateModel);
                        } catch (Exception e) {
                            log.error("Failed to send overdue email for bill #{}: {}", bill.getId(), e.getMessage());
                        }
                    }
                });
    }
}