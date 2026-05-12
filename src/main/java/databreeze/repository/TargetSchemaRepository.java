package databreeze.repository;

import databreeze.entity.TargetSchema;
import databreeze.enums.DataSourceType;
import databreeze.enums.SourcePlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TargetSchemaRepository extends JpaRepository<TargetSchema, UUID> {
    Optional<TargetSchema> findFirstByPlatformAndDataSourceTypeAndIsActiveTrueOrderByVersionDesc(SourcePlatform platform, DataSourceType dataSourceType);
    Optional<TargetSchema> findByCodeAndIsActiveTrue(String code);
}
