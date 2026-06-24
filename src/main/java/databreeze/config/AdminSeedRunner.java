package databreeze.config;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import databreeze.entity.User;
import databreeze.enums.AuthProvider;
import databreeze.enums.SystemRole;
import databreeze.enums.UserStatus;
import databreeze.enums.UserType;
import databreeze.repository.UserRepository;

@Component
public class AdminSeedRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminSeedRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.seed.enabled:false}")
    private boolean seedEnabled;

    @Value("${app.admin.seed.email:databreeze.team@gmail.com}")
    private String seedEmail;

    @Value("${app.admin.seed.password:Admin@123456}")
    private String seedPassword;

    @Value("${app.admin.seed.full-name:System Admin}")
    private String seedFullName;

    public AdminSeedRunner(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        String email = normalizeEmail(seedEmail);
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("app.admin.seed.email khong duoc de trong");
        }
        if (seedPassword == null || seedPassword.isBlank()) {
            throw new IllegalStateException("app.admin.seed.password khong duoc de trong");
        }

        User existing = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (existing != null) {
            if (existing.getSystemRole() != SystemRole.ADMIN && existing.getSystemRole() != SystemRole.SUPER_ADMIN) {
                existing.setSystemRole(SystemRole.ADMIN);
                existing.setStatus(UserStatus.ACTIVE);
                userRepository.save(existing);
                logger.info("Promoted existing user to ADMIN: {}", email);
            }
            return;
        }

        User admin = User.builder()
                .email(email)
                .fullName(seedFullName == null ? null : seedFullName.trim())
                .authProvider(AuthProvider.EMAIL_PASSWORD)
                .emailVerified(true)
                .userType(UserType.PERSONAL)
                .systemRole(SystemRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .passwordHash(passwordEncoder.encode(seedPassword))
                .build();

        userRepository.save(admin);
        logger.info("Seeded admin account: {}", email);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
