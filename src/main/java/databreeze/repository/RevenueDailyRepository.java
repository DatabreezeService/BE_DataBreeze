package databreeze.repository;

import databreeze.entity.RevenueDaily;
import databreeze.enums.SourcePlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RevenueDailyRepository extends JpaRepository<RevenueDaily, UUID> {
    @Modifying
    @Query("""
            delete from RevenueDaily r
            where r.workspaceId = :workspaceId
              and r.platform = :platform
              and ((:storeId is null and r.storeId is null) or r.storeId = :storeId)
              and r.revenueDate between :from and :to
            """)
    int deleteRange(
            @Param("workspaceId") UUID workspaceId,
            @Param("platform") SourcePlatform platform,
            @Param("storeId") UUID storeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    List<RevenueDaily> findByWorkspaceIdAndPlatformOrderByRevenueDateDesc(UUID workspaceId, SourcePlatform platform);
}
