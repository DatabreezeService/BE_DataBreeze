package databreeze.service.payments;

import java.util.Map;
import java.util.UUID;

import databreeze.dto.payments.CreatePaymentLinkRequestBody;
import databreeze.entity.PaymentTransaction;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.WebhookData;

public interface PaymentTransactionService {

    PaymentTransaction recordCreatedPayment(
            UUID actorUserId,
            CreatePaymentLinkRequestBody requestBody,
            long fallbackOrderCode,
            CreatePaymentLinkResponse response);

    PaymentTransaction applyPayosWebhook(WebhookData data, Map<String, Object> payload);
}
