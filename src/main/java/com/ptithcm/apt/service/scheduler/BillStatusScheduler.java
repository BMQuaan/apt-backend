package com.ptithcm.apt.service.scheduler;

import com.ptithcm.apt.entity.Bill;
import com.ptithcm.apt.enums.BillStatus;
import com.ptithcm.apt.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                LocalDateTime.now()
        );

        if (!overdueBills.isEmpty()) {
            overdueBills.forEach(bill -> {
                bill.setStatus(BillStatus.LATE);
                log.info("Bill #{} (Apt: {}) is overdue. Status changed to LATE.", 
                         bill.getId(), bill.getApartment().getRoomNumber());
            });

            billRepository.saveAll(overdueBills);
            log.info("Successfully updated {} bills to LATE.", overdueBills.size());
        } else {
            log.info("No overdue bills found today.");
        }
    }
}