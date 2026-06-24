package databreeze.service.payments.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import databreeze.dto.payments.CreatePaymentLinkRequestBody;
import databreeze.entity.PaymentTransaction;
import databreeze.entity.User;
import databreeze.entity.Workspace;
import databreeze.enums.PaymentProvider;
import databreeze.enums.PaymentStatus;
import databreeze.repository.PaymentTransactionRepository;
import databreeze.repository.UserRepository;
import databreeze.service.payments.PaymentTransactionService;
import databreeze.service.workspace.WorkspaceAccessService;
import databreeze.service.workspace.WorkspaceBootstrapService;
import lombok.RequiredArgsConstructor;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.webhooks.WebhookData;

@Service
@RequiredArgsConstructor
public class PaymentTransactionServiceImpl implements PaymentTransactionService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccessService workspaceAccessService;
    private final WorkspaceBootstrapService workspaceBootstrapService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PaymentTransaction recordCreatedPayment(
            UUID actorUserId,
            CreatePaymentLinkRequestBody requestBody,
            long fallbackOrderCode,
            CreatePaymentLinkResponse response) {
        Workspace workspace = resolveWorkspace(actorUserId, requestBody.getWorkspaceId());
        long orderCode = response.getOrderCode() == null ? fallbackOrderCode : response.getOrderCode();
        long amount = response.getAmount() == null ? requestBody.getPrice() : response.getAmount();

        PaymentTransaction transaction = PaymentTransaction.builder()
                .workspaceId(workspace.getId())
                .provider(PaymentProvider.PAYOS)
                .providerPaymentId(response.getPaymentLinkId())
                .providerOrderCode(String.valueOf(orderCode))
                .amount(BigDecimal.valueOf(amount))
                .currencyCode(firstNonBlank(response.getCurrency(), "VND"))
                .status(toPaymentStatus(response.getStatus()))
                .checkoutUrl(response.getCheckoutUrl())
                .qrCode(response.getQrCode())
                .returnUrl(requestBody.getReturnUrl())
                .cancelUrl(requestBody.getCancelUrl())
                .expiredAt(toOffsetDateTime(response.getExpiredAt()))
                .providerPayload(objectMapper.convertValue(response, MAP_TYPE))
                .build();

        return paymentTransactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public PaymentTransaction applyPayosWebhook(WebhookData data, Map<String, Object> payload) {
        PaymentTransaction transaction = findPayosTransaction(data)
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay giao dich PayOS tu webhook"));

        if (data.getPaymentLinkId() != null && !data.getPaymentLinkId().isBlank()) {
            transaction.setProviderPaymentId(data.getPaymentLinkId());
        }
        if (data.getOrderCode() != null) {
            transaction.setProviderOrderCode(String.valueOf(data.getOrderCode()));
        }
        if (data.getAmount() != null) {
            transaction.setAmount(BigDecimal.valueOf(data.getAmount()));
        }
        if (data.getCurrency() != null && !data.getCurrency().isBlank()) {
            transaction.setCurrencyCode(data.getCurrency());
        }

        transaction.setWebhookPayload(objectMapper.convertValue(payload == null ? Map.of() : payload, MAP_TYPE));
        if ("00".equals(data.getCode())) {
            transaction.setStatus(PaymentStatus.PAID);
            transaction.setPaidAt(parsePayosTransactionTime(data.getTransactionDateTime()));
        } else {
            transaction.setStatus(PaymentStatus.FAILED);
        }

        return paymentTransactionRepository.save(transaction);
    }

    private Workspace resolveWorkspace(UUID actorUserId, UUID requestedWorkspaceId) {
        if (requestedWorkspaceId != null) {
            return workspaceAccessService.requireReadAccess(requestedWorkspaceId, actorUserId);
        }

        User user = userRepository.findById(actorUserId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay user dang dang nhap"));
        return workspaceBootstrapService.getOrCreatePersonalWorkspace(user);
    }

    private Optional<PaymentTransaction> findPayosTransaction(WebhookData data) {
        if (data.getPaymentLinkId() != null && !data.getPaymentLinkId().isBlank()) {
            Optional<PaymentTransaction> byPaymentId = paymentTransactionRepository
                    .findByProviderAndProviderPaymentId(PaymentProvider.PAYOS, data.getPaymentLinkId());
            if (byPaymentId.isPresent()) {
                return byPaymentId;
            }
        }

        if (data.getOrderCode() != null) {
            return paymentTransactionRepository.findByProviderAndProviderOrderCode(
                    PaymentProvider.PAYOS, String.valueOf(data.getOrderCode()));
        }

        return Optional.empty();
    }

    private PaymentStatus toPaymentStatus(PaymentLinkStatus status) {
        if (status == null) {
            return PaymentStatus.PENDING;
        }

        return switch (status.name()) {
            case "PAID" -> PaymentStatus.PAID;
            case "CANCELLED" -> PaymentStatus.CANCELLED;
            case "EXPIRED" -> PaymentStatus.EXPIRED;
            case "PENDING" -> PaymentStatus.PENDING;
            default -> PaymentStatus.PROCESSING;
        };
    }

    private OffsetDateTime toOffsetDateTime(Long epochSeconds) {
        if (epochSeconds == null) {
            return null;
        }

        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
    }

    private OffsetDateTime parsePayosTransactionTime(String value) {
        if (value == null || value.isBlank()) {
            return OffsetDateTime.now(VIETNAM_ZONE);
        }

        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
        }

        for (DateTimeFormatter formatter : payosLocalDateTimeFormatters()) {
            try {
                return LocalDateTime.parse(value, formatter).atZone(VIETNAM_ZONE).toOffsetDateTime();
            } catch (DateTimeParseException ignored) {
            }
        }

        return OffsetDateTime.now(VIETNAM_ZONE);
    }

    private java.util.List<DateTimeFormatter> payosLocalDateTimeFormatters() {
        return java.util.List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
