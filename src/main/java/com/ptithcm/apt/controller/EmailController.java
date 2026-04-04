package com.ptithcm.apt.controller;

import com.ptithcm.apt.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    // Test gửi email đơn giản
    @PostMapping("/send")
    public String sendEmail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String text
    ) {
        emailService.sendSimpleEmail(to, subject, text);
        return "Gửi email thành công!";
    }

    // Test gửi email HTML
    @PostMapping("/send-template")
    public String sendTemplateEmail(@RequestBody Map<String, Object> request) {

        String to = (String) request.get("to");
        String subject = (String) request.get("subject");
        String template = (String) request.get("template");

        Map<String, String> variables = (Map<String, String>) request.get("variables");

        emailService.sendHtmlEmail(to, subject, template, variables);

        return "Gửi email template thành công!";
    }
}