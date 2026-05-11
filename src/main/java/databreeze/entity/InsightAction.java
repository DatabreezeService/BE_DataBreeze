package databreeze.entity;

import databreeze.enums.InsightActionStatus;
import databreeze.enums.InsightActionType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "insight_actions")
public class InsightAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "insight_id", nullable = false)
    private UUID insightId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private InsightActionType actionType;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_status", nullable = false, length = 50)
    @Builder.Default
    private InsightActionStatus actionStatus = InsightActionStatus.SUGGESTED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
