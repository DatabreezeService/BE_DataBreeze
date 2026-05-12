package databreeze.repository;

import databreeze.entity.RevenueDaily;
import databreeze.enums.SourcePlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.UUID;

public interface RevenueDailyRepository extends JpaRepository<RevenueDaily, UUID> {
    @Modifying
    @Query("delete from RevenueDaily r where r.workspaceId = :workspaceId and r.platform = :platform and r.revenueDate between :from and :to")
    int deleteRange(@Param("workspaceId") UUID workspaceId, @Param("platform") SourcePlatform platform, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
