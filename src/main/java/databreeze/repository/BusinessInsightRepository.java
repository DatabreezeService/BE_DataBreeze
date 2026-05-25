package databreeze.repository;

import databreeze.entity.BusinessInsight;
import databreeze.enums.InsightGeneratedBy;
import databreeze.enums.InsightStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BusinessInsightRepository extends JpaRepository<BusinessInsight, UUID> {
    List<BusinessInsight> findByWorkspaceIdOrderByGeneratedAtDesc(UUID workspaceId);

    List<BusinessInsight> findByWorkspaceIdAndStatusOrderByGeneratedAtDesc(UUID workspaceId, InsightStatus status);

    @Modifying
    @Query("""
            delete from BusinessInsight i
            where i.workspaceId = :workspaceId
              and ((:storeId is null and i.storeId is null) or i.storeId = :storeId)
              and i.periodStart = :periodStart
              and i.periodEnd = :periodEnd
              and i.generatedBy = :generatedBy
            """)
    int deleteGeneratedForPeriod(
            @Param("workspaceId") UUID workspaceId,
            @Param("storeId") UUID storeId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("generatedBy") InsightGeneratedBy generatedBy
    );
}
