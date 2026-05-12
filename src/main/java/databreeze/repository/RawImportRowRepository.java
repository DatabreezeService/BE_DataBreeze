package databreeze.repository;

import databreeze.entity.RawImportRow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RawImportRowRepository extends JpaRepository<RawImportRow, UUID> {
    List<RawImportRow> findByImportJobIdOrderByRowNumberAsc(UUID importJobId);

    long countByImportJobId(UUID importJobId);

    long countByImportJobIdAndStatus(UUID importJobId, databreeze.enums.RawRowStatus status);
}
