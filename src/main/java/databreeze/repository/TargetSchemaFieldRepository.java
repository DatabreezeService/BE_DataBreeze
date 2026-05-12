package databreeze.repository;

import databreeze.entity.TargetSchemaField;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TargetSchemaFieldRepository extends JpaRepository<TargetSchemaField, UUID> {
    List<TargetSchemaField> findByTargetSchemaIdAndIsActiveTrueOrderByDisplayOrderAsc(UUID targetSchemaId);
    Optional<TargetSchemaField> findByTargetSchemaIdAndFieldNameAndIsActiveTrue(UUID targetSchemaId, String fieldName);

}
