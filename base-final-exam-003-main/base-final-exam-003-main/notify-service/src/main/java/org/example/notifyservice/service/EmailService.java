package org.example.notifyservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${notification.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendEnrollmentCreatedEmail(String recipient) {
        String trimmedRecipient = recipient.trim();
        if (!StringUtils.hasText(trimmedRecipient)) {
            throw new IllegalArgumentException("Recipient email cannot be blank");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(trimmedRecipient);
        message.setSubject("Đăng ký khóa học thành công");
        message.setText("Bạn đã đăng ký khóa học thành công. Cảm ơn bạn đã tham gia!");

        mailSender.send(message);
    }
}
