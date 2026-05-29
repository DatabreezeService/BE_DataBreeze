package databreeze.repository;

import databreeze.entity.ProfitDaily;
import databreeze.enums.SourcePlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProfitDailyRepository extends JpaRepository<ProfitDaily, UUID> {
    @Modifying
    @Query("""
            delete from ProfitDaily p
            where p.workspaceId = :workspaceId
              and p.platform = :platform
              and ((:storeId is null and p.storeId is null) or p.storeId = :storeId)
              and p.profitDate between :from and :to
            """)
    int deleteRange(
            @Param("workspaceId") UUID workspaceId,
            @Param("platform") SourcePlatform platform,
            @Param("storeId") UUID storeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    List<ProfitDaily> findByWorkspaceIdAndPlatformOrderByProfitDateDesc(UUID workspaceId, SourcePlatform platform);

}
