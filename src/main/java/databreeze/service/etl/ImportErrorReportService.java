package databreeze.service.etl;

import databreeze.entity.ImportJob;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public interface ImportErrorReportService {
    Optional<String> generateReport(ImportJob job) throws IOException;

    Path resolveReport(UUID workspaceId, UUID actorUserId, UUID importJobId);
}
