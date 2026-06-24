package databreeze.service.payments;

import java.util.Map;
import java.util.UUID;

import databreeze.entity.PaymentWebhookEvent;
import vn.payos.model.webhooks.WebhookData;

public interface PaymentWebhookEventService {

    PaymentWebhookEvent recordPayosReceived(Map<String, Object> payload);

    void markPayosInvalid(PaymentWebhookEvent event);

    void markPayosProcessed(PaymentWebhookEvent event, WebhookData data, UUID paymentTransactionId);

    void markPayosFailed(PaymentWebhookEvent event, WebhookData data);
}
