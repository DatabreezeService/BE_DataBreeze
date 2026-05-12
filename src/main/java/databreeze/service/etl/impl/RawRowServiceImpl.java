package databreeze.service.etl.impl;

import databreeze.entity.RawImportRow;
import databreeze.enums.RawRowStatus;
import databreeze.repository.RawImportRowRepository;
import databreeze.service.etl.ParsedFile;
import databreeze.service.etl.RawRowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Implementation lưu và đọc raw_import_rows.
 */
@Service
public class RawRowServiceImpl implements RawRowService {
    @Autowired
    private RawImportRowRepository rawRowRepository;

    @Override
    @Transactional
    public void saveRawRows(UUID workspaceId, UUID uploadId, UUID importJobId, ParsedFile parsedFile) {
        List<RawImportRow> rows = new ArrayList<>();
        long rowNumber = 2; // dòng 1 là header trong Excel/CSV
        for (Map<String, Object> row : parsedFile.getRows()) {
            rows.add(RawImportRow.builder()
                    .workspaceId(workspaceId)
                    .uploadId(uploadId)
                    .importJobId(importJobId)
                    .rowNumber(rowNumber++)
                    .rawData(row)
                    .rowHash(sha256(row.toString()))
                    .status(RawRowStatus.VALID)
                    .build());
        }
        rawRowRepository.saveAll(rows);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RawImportRow> findRows(UUID importJobId) {
        return rawRowRepository.findByImportJobIdOrderByRowNumberAsc(importJobId);
    }

    @Override
    @Transactional(readOnly = true)
    public ParsedFile toParsedFile(UUID importJobId) {
        List<RawImportRow> rawRows = findRows(importJobId);
        if (rawRows.isEmpty()) return new ParsedFile(List.of(), List.of());
        List<String> headers = new ArrayList<>(rawRows.get(0).getRawData().keySet());
        List<Map<String, Object>> rows = rawRows.stream().map(RawImportRow::getRawData).toList();
        return new ParsedFile(headers, rows);
    }

    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) builder.append(String.format("%02x", b));
            return builder.toString();
        } catch (Exception ex) {
            return UUID.randomUUID().toString();
        }
    }
}
