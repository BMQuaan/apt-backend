package com.ptithcm.apt.service.impl;

import com.ptithcm.apt.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Override
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            javaMailSender.send(message);
            log.info("Đã gửi email thành công đến: {}", to);

        } catch (Exception e) {
            log.error("Lỗi khi gửi email đến {}: {}", to, e.getMessage());
            throw new RuntimeException("Không thể gửi email lúc này, vui lòng thử lại sau.");
        }
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlFileName, Map<String, String> templateModel) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String htmlContent = loadHtmlTemplate(htmlFileName);

            if (templateModel != null && !templateModel.isEmpty()) {
                for (Map.Entry<String, String> entry : templateModel.entrySet()) {
                    String placeholder = "{{" + entry.getKey() + "}}";
                    htmlContent = htmlContent.replace(placeholder, entry.getValue());
                }
            }

            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("Đã gửi HTML email thành công đến: {}", to);

        } catch (MessagingException e) {
            log.error("Lỗi khi gửi HTML email đến {}: {}", to, e.getMessage());
            throw new RuntimeException("Lỗi hệ thống khi gửi email, vui lòng thử lại sau.");
        }
    }

    public String loadHtmlTemplate(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/" + fileName);
            try (InputStream inputStream = resource.getInputStream()) {
                byte[] bytes = inputStream.readAllBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("Không đọc được file HTML template", e);
        }
    }
}