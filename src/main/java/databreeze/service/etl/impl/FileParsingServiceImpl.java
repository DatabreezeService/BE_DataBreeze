package databreeze.service.etl.impl;

import databreeze.service.etl.FileParsingService;
import databreeze.service.etl.ParsedFile;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Implementation đọc Excel/CSV.
 * MVP đang ưu tiên Shopee VN nên xử lý tốt header tiếng Việt, UTF-8 và giá trị ngày/tháng.
 */
@Service
public class FileParsingServiceImpl implements FileParsingService {
    private static final int MAX_ROWS_PER_UPLOAD = 5000;

    /**
     * Tự nhận diện loại file theo extension.
     * .xlsx/.xls dùng Apache POI; còn lại xử lý như CSV UTF-8.
     */
    @Override
    public ParsedFile parse(MultipartFile file) throws IOException {
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) return parseExcel(file);
        if (name.endsWith(".csv")) return parseCsv(file);
        throw new IllegalArgumentException("Định dạng file chưa hỗ trợ. Vui lòng upload Excel (.xlsx/.xls) hoặc CSV (.csv).");
    }

    /**
     * Đọc CSV UTF-8, dòng đầu tiên là header.
     * Shopee VN export thường dùng header tiếng Việt nên giữ nguyên tên cột gốc để mapping.
     */
    private ParsedFile parseCsv(MultipartFile file) throws IOException {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build().parse(reader)) {
            List<String> headers = new ArrayList<>(parser.getHeaderNames());
            List<Map<String, Object>> rows = new ArrayList<>();
            int count = 0;
            for (CSVRecord record : parser) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String h : headers) row.put(h, record.get(h));
                rows.add(row);
                if (++count >= MAX_ROWS_PER_UPLOAD) break;
            }
            return new ParsedFile(headers, rows);
        }
    }

    /**
     * Đọc sheet đầu tiên của Excel.
     * Dòng đầu tiên là header; các dòng trống bị bỏ qua.
     */
    private ParsedFile parseExcel(MultipartFile file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = sheet.rowIterator();
            if (!iterator.hasNext()) return new ParsedFile(List.of(), List.of());

            DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("vi-VN"));
            Row headerRow = iterator.next();
            int lastCell = headerRow.getLastCellNum();
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < lastCell; i++) {
                String value = formatter.formatCellValue(headerRow.getCell(i)).trim();
                if (!value.isBlank()) headers.add(value);
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            int count = 0;
            while (iterator.hasNext() && count < MAX_ROWS_PER_UPLOAD) {
                Row row = iterator.next();
                Map<String, Object> map = new LinkedHashMap<>();
                boolean blank = true;
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    Object value = cellValue(cell, formatter);
                    if (value != null && !value.toString().isBlank()) blank = false;
                    map.put(headers.get(i), value);
                }
                if (!blank) { rows.add(map); count++; }
            }
            return new ParsedFile(headers, rows);
        }
    }

    /**
     * Chuyển value từ Excel cell sang Java object đơn giản.
     * Date cell được giữ dạng LocalDate để service ETL convert tiếp sang OffsetDateTime khi import.
     */
    private Object cellValue(Cell cell, DataFormatter formatter) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case BOOLEAN -> cell.getBooleanCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? LocalDateTime.ofInstant(cell.getDateCellValue().toInstant(), ZoneId.systemDefault()).toLocalDate()
                    : cell.getNumericCellValue();
            case FORMULA -> formatter.formatCellValue(cell);
            case BLANK, _NONE, ERROR -> null;
        };
    }
}
