package databreeze.service.auth.impl;

import databreeze.service.auth.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
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
    private final boolean mailEnabled;
    private final String mailHost;
    private final String mailUsername;

    public EmailServiceImpl(JavaMailSender mailSender,
                            SpringTemplateEngine templateEngine,
                            @Value("${app.mail.from}") String mailFrom,
                            @Value("${app.mail.enabled:true}") boolean mailEnabled,
                            @Value("${spring.mail.host:}") String mailHost,
                            @Value("${spring.mail.username:}") String mailUsername) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.mailFrom = mailFrom;
        this.mailEnabled = mailEnabled;
        this.mailHost = mailHost;
        this.mailUsername = mailUsername;
    }

    @Override
    public void sendVerificationOtp(String email, String fullName, String otp, OffsetDateTime expiresAt) {
        if (!mailEnabled) {
            throw new IllegalStateException("Dich vu email dang tat. Bat APP_MAIL_ENABLED=true hoac tat APP_AUTH_REQUIRE_EMAIL_VERIFICATION de test.");
        }
        if (mailHost == null || mailHost.isBlank() || mailUsername == null || mailUsername.isBlank()) {
            throw new IllegalStateException("Chua cau hinh SMTP. Vui long set MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD va MAIL_FROM.");
        }

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
        } catch (MessagingException | MailException ex) {
            throw new IllegalStateException("Khong gui duoc email OTP. Vui long kiem tra SMTP MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD/MAIL_FROM.", ex);
        }
    }
}
