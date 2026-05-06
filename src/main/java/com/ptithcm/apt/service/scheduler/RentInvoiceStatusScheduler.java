package com.ptithcm.apt.service.scheduler;

import com.ptithcm.apt.entity.RentInvoice;
import com.ptithcm.apt.enums.RentStatus;
import com.ptithcm.apt.repository.RentInvoiceRepository;
import com.ptithcm.apt.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RentInvoiceStatusScheduler {

    private final RentInvoiceRepository rentInvoiceRepository;
    private final EmailService emailService;

    /**
     * Tự động kiểm tra khi ứng dụng vừa khởi động xong.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationStart() {
        log.info(">>> RentInvoice system started. Checking for overdue rent invoices...");
        autoUpdateRentLateStatus();
    }

    /**
     * Chạy mỗi ngày vào lúc 00:00:00 để quét các hóa đơn quá hạn.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void autoUpdateRentLateStatus() {
        log.info("Cron Job: Checking for overdue RENT invoices at {}", LocalDateTime.now());

        List<RentInvoice> overdueInvoices = rentInvoiceRepository.findAllByStatusAndDueDateBefore(
                RentStatus.UNPAID,
                LocalDateTime.now());

        if (!overdueInvoices.isEmpty()) {
            for (RentInvoice rent : overdueInvoices) {

                rent.setStatus(RentStatus.LATE);

                sendRentOverdueEmail(rent);

                log.info("Rent Invoice #{} (Apt: {}) is overdue. Updated to LATE.",
                        rent.getId(), rent.getApartment().getRoomNumber());
            }
            rentInvoiceRepository.saveAll(overdueInvoices);
            log.info("Successfully notified {} rent invoices as LATE.", overdueInvoices.size());
        } else {
            log.info("No overdue rent invoices found today.");
        }
    }

    private void sendRentOverdueEmail(RentInvoice rent) {
        if (rent.getTenant() != null && rent.getTenant().getEmail() != null) {
            try {
                Map<String, String> templateModel = Map.of(
                        "fullName", rent.getTenant().getFullName(),
                        "roomNumber", rent.getApartment().getRoomNumber(),
                        "month", String.valueOf(rent.getBillingMonth()),
                        "year", String.valueOf(rent.getBillingYear()),
                        "totalAmount", String.format("%,.0f", rent.getRentAmount()),
                        "dueDate", rent.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

                emailService.sendHtmlEmail(
                        rent.getTenant().getEmail(),
                        "[AptApp] CẢNH BÁO QUÁ HẠN TIỀN THUÊ NHÀ - Căn hộ " + rent.getApartment().getRoomNumber(),
                        "overdue_rent_invoice_template_vi.html",
                        templateModel);
            } catch (Exception e) {
                log.error("Failed to send rent overdue email for ID #{}: {}", rent.getId(), e.getMessage());
            }
        }
    }
}