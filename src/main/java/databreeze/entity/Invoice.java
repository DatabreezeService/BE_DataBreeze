package databreeze.entity;

import databreeze.enums.InvoiceStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 100)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "subtotal_amount")
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "tax_amount")
    private BigDecimal taxAmount;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "currency_code", nullable = false, length = 10)
    @Builder.Default
    private String currencyCode = "VND";

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "due_at")
    private OffsetDateTime dueAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
