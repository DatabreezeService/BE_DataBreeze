package databreeze.service.auth.impl;

import databreeze.service.auth.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final String mailFrom;

    public EmailServiceImpl(JavaMailSender mailSender,
                            SpringTemplateEngine templateEngine,
                            @Value("${app.mail.from}") String mailFrom) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.mailFrom = mailFrom;
    }

    @Override
    public void sendVerificationOtp(String email, String fullName, String otp, OffsetDateTime expiresAt) {
        Context context = new Context();
        context.setVariable("fullName", fullName == null || fullName.isBlank() ? email : fullName);
        context.setVariable("otp", otp);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        context.setVariable("expiresAtText", expiresAt == null ? "" : formatter.format(expiresAt));

        String html = templateEngine.process("emails/verify-otp", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setTo(email);
            helper.setSubject("DataBreeze - Ma OTP xac thuc email");
            helper.setText(html, true);
            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom);
            }
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Gửi email xác thực thất bại. Vui lòng thử lại.", ex);
        }
    }
}
