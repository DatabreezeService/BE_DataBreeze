package databreeze.service.payments.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import databreeze.entity.PaymentWebhookEvent;
import databreeze.enums.PaymentProvider;
import databreeze.enums.WebhookProcessingStatus;
import databreeze.enums.WebhookVerificationStatus;
import databreeze.repository.PaymentWebhookEventRepository;
import databreeze.service.payments.PaymentWebhookEventService;
import lombok.RequiredArgsConstructor;
import vn.payos.model.webhooks.WebhookData;

@Service
@RequiredArgsConstructor
public class PaymentWebhookEventServiceImpl implements PaymentWebhookEventService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final PaymentWebhookEventRepository paymentWebhookEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PaymentWebhookEvent recordPayosReceived(Map<String, Object> payload) {
        Map<String, Object> normalizedPayload = payload == null ? Map.of() : objectMapper.convertValue(payload, MAP_TYPE);

        PaymentWebhookEvent event = PaymentWebhookEvent.builder()
                .provider(PaymentProvider.PAYOS)
                .eventType("payos.webhook")
                .providerEventId(providerEventIdFromPayload(normalizedPayload))
                .signature(stringValue(normalizedPayload.get("signature")))
                .payload(normalizedPayload)
                .verificationStatus(WebhookVerificationStatus.PENDING)
                .processingStatus(WebhookProcessingStatus.RECEIVED)
                .receivedAt(now())
                .build();

        return paymentWebhookEventRepository.save(event);
    }

    @Override
    @Transactional
    public void markPayosInvalid(PaymentWebhookEvent event) {
        event.setVerificationStatus(WebhookVerificationStatus.INVALID);
        event.setProcessingStatus(WebhookProcessingStatus.FAILED);
        event.setProcessedAt(now());
        paymentWebhookEventRepository.save(event);
    }

    @Override
    @Transactional
    public void markPayosProcessed(PaymentWebhookEvent event, WebhookData data, UUID paymentTransactionId) {
        fillVerifiedPayosDetails(event, data);
        event.setPaymentTransactionId(paymentTransactionId);
        event.setProcessingStatus(WebhookProcessingStatus.PROCESSED);
        event.setProcessedAt(now());
        paymentWebhookEventRepository.save(event);
    }

    @Override
    @Transactional
    public void markPayosFailed(PaymentWebhookEvent event, WebhookData data) {
        fillVerifiedPayosDetails(event, data);
        event.setProcessingStatus(WebhookProcessingStatus.FAILED);
        event.setProcessedAt(now());
        paymentWebhookEventRepository.save(event);
    }

    private void fillVerifiedPayosDetails(PaymentWebhookEvent event, WebhookData data) {
        event.setVerificationStatus(WebhookVerificationStatus.VALID);
        event.setEventType("payos.payment");
        event.setProviderEventId(providerEventIdFromData(data));
    }

    private String providerEventIdFromPayload(Map<String, Object> payload) {
        Object data = payload.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            String reference = stringValue(dataMap.get("reference"));
            if (reference != null) {
                return reference;
            }

            String paymentLinkId = stringValue(dataMap.get("paymentLinkId"));
            if (paymentLinkId != null) {
                return paymentLinkId;
            }

            String orderCode = stringValue(dataMap.get("orderCode"));
            if (orderCode != null) {
                return orderCode;
            }
        }

        return stringValue(payload.get("signature"));
    }

    private String providerEventIdFromData(WebhookData data) {
        if (data == null) {
            return null;
        }

        if (data.getReference() != null && !data.getReference().isBlank()) {
            return data.getReference();
        }
        if (data.getPaymentLinkId() != null && !data.getPaymentLinkId().isBlank()) {
            return data.getPaymentLinkId();
        }
        return data.getOrderCode() == null ? null : String.valueOf(data.getOrderCode());
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.ofHours(7));
    }
}
