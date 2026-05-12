package databreeze.repository;

import databreeze.entity.ImportColumnMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ImportColumnMappingRepository extends JpaRepository<ImportColumnMapping, UUID> {

    List<ImportColumnMapping> findByImportJobId(UUID importJobId);

    List<ImportColumnMapping> findByImportJobIdAndUserConfirmedTrue(UUID importJobId);

    void deleteByImportJobId(UUID importJobId);
}