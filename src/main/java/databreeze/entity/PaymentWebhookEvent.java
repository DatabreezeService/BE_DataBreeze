package databreeze.entity;

import databreeze.entity.enums.PaymentProvider;
import databreeze.entity.enums.WebhookProcessingStatus;
import databreeze.entity.enums.WebhookVerificationStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_webhook_events")
public class PaymentWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    @Builder.Default
    private PaymentProvider provider = PaymentProvider.PAYOS;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(name = "provider_event_id", length = 255)
    private String providerEventId;

    @Column(name = "payment_transaction_id")
    private UUID paymentTransactionId;

    @Column(name = "signature", length = 500)
    private String signature;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 50)
    @Builder.Default
    private WebhookVerificationStatus verificationStatus = WebhookVerificationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 50)
    @Builder.Default
    private WebhookProcessingStatus processingStatus = WebhookProcessingStatus.RECEIVED;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

}
