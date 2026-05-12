package databreeze.entity;

import databreeze.enums.AllocationMethod;
import databreeze.enums.ExpenseCategory;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "operating_expenses")
public class OperatingExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    @Builder.Default
    private ExpenseCategory category = ExpenseCategory.OTHER;

    @Column(name = "description")
    private String description;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 10)
    @Builder.Default
    private String currencyCode = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_method", length = 50)
    @Builder.Default
    private AllocationMethod allocationMethod = AllocationMethod.MANUAL;

    @Column(name = "source_upload_id")
    private UUID sourceUploadId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
