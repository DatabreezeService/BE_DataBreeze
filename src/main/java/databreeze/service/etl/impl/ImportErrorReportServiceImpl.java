package databreeze.service.etl.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import databreeze.entity.ImportJob;
import databreeze.entity.RawImportRow;
import databreeze.enums.RawRowStatus;
import databreeze.repository.ImportJobRepository;
import databreeze.repository.RawImportRowRepository;
import databreeze.service.etl.ImportErrorReportService;
import databreeze.service.workspace.WorkspaceAccessService;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportErrorReportServiceImpl implements ImportErrorReportService {
    @Autowired
    private RawImportRowRepository rawImportRowRepository;

    @Autowired
    private ImportJobRepository importJobRepository;

    @Autowired
    private WorkspaceAccessService workspaceAccessService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.storage.local-dir:./storage/uploads}")
    private String storageDir;

    @Override
    @Transactional(readOnly = true)
    public Optional<String> generateReport(ImportJob job) throws IOException {
        List<RawImportRow> rows = rawImportRowRepository.findByImportJobIdOrderByRowNumberAsc(job.getId()).stream()
                .filter(this::shouldExport)
                .toList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }

        Path reportDir = Paths.get(storageDir).resolve("error-reports").normalize();
        Files.createDirectories(reportDir);
        String fileName = "import-" + job.getId() + "-row-errors.xlsx";
        Path reportPath = reportDir.resolve(fileName).normalize();

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(reportPath)) {
            Sheet sheet = workbook.createSheet("Row errors");
            sheet.createFreezePane(0, 1);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook);

            List<String> rawHeaders = collectRawHeaders(rows);
            List<String> headers = new ArrayList<>(List.of(
                    "source_row_number",
                    "status",
                    "loai_loi",
                    "noi_dung_loi",
                    "canh_bao",
                    "du_lieu_goc_json"
            ));
            headers.addAll(rawHeaders);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (RawImportRow rawRow : rows) {
                Row excelRow = sheet.createRow(rowIndex++);
                writeCell(excelRow, 0, rawRow.getRowNumber(), bodyStyle);
                writeCell(excelRow, 1, rawRow.getStatus() == null ? "" : rawRow.getStatus().name(), bodyStyle);
                writeCell(excelRow, 2, errorType(rawRow), bodyStyle);
                writeCell(excelRow, 3, toJson(rawRow.getErrorMessages()), bodyStyle);
                writeCell(excelRow, 4, toJson(rawRow.getWarningMessages()), bodyStyle);
                writeCell(excelRow, 5, toJson(rawRow.getRawData()), bodyStyle);
                for (int i = 0; i < rawHeaders.size(); i++) {
                    Object value = rawRow.getRawData() == null ? null : rawRow.getRawData().get(rawHeaders.get(i));
                    writeCell(excelRow, i + 6, value, bodyStyle);
                }
            }

            for (int i = 0; i < Math.min(headers.size(), 20); i++) {
                sheet.autoSizeColumn(i);
                int width = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(Math.max(width, 3000), 12000));
            }

            workbook.write(outputStream);
        }

        return Optional.of("error-reports/" + fileName);
    }

    @Override
    @Transactional(readOnly = true)
    public Path resolveReport(UUID workspaceId, UUID actorUserId, UUID importJobId) {
        workspaceAccessService.requireReadAccess(workspaceId, actorUserId);
        ImportJob job = importJobRepository.findById(importJobId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy import job."));
        if (!workspaceId.equals(job.getWorkspaceId())) {
            throw new SecurityException("Import job không thuộc workspace hiện tại.");
        }
        if (job.getErrorReportStorageKey() == null || job.getErrorReportStorageKey().isBlank()) {
            throw new NoSuchElementException("Import job chưa có file Excel lỗi.");
        }
        Path baseDir = Paths.get(storageDir).toAbsolutePath().normalize();
        Path reportPath = baseDir.resolve(job.getErrorReportStorageKey()).normalize();
        if (!reportPath.startsWith(baseDir) || !Files.exists(reportPath)) {
            throw new NoSuchElementException("Không tìm thấy file Excel lỗi trên server local.");
        }
        return reportPath;
    }

    private boolean shouldExport(RawImportRow row) {
        return row.getStatus() == RawRowStatus.INVALID
                || row.getStatus() == RawRowStatus.SKIPPED
                || row.getStatus() == RawRowStatus.WARNING
                || row.getStatus() == RawRowStatus.DUPLICATE;
    }

    private List<String> collectRawHeaders(List<RawImportRow> rows) {
        Set<String> headers = new LinkedHashSet<>();
        for (RawImportRow row : rows) {
            if (row.getRawData() != null) {
                headers.addAll(row.getRawData().keySet());
            }
        }
        return new ArrayList<>(headers);
    }

    private String errorType(RawImportRow row) {
        if (row.getStatus() == RawRowStatus.WARNING) {
            return "CANH_BAO_DU_LIEU";
        }
        if (row.getStatus() == RawRowStatus.DUPLICATE) {
            return "TRUNG_DU_LIEU";
        }
        if (row.getStatus() == RawRowStatus.SKIPPED) {
            return "DA_BO_QUA";
        }
        return "LOI_IMPORT";
    }

    private String toJson(Object value) {
        if (value == null) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    private void writeCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value == null ? "" : String.valueOf(value));
        }
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createBodyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        return style;
    }
}
