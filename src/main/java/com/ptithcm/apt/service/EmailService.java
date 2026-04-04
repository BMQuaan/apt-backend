package com.ptithcm.apt.service;

import java.util.Map;

public interface EmailService {
    void sendSimpleEmail(String to, String subject, String text);

    // Truyền thêm templateModel (chứa các biến động)
    void sendHtmlEmail(String to, String subject, String htmlFileName, Map<String, String> templateModel);
}