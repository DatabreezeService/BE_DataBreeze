package databreeze.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import databreeze.entity.PaymentWebhookEvent;
import databreeze.enums.PaymentProvider;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> {

    List<PaymentWebhookEvent> findByProviderAndProviderEventIdOrderByReceivedAtDesc(
            PaymentProvider provider, String providerEventId);

    List<PaymentWebhookEvent> findByPaymentTransactionIdOrderByReceivedAtDesc(UUID paymentTransactionId);
}
