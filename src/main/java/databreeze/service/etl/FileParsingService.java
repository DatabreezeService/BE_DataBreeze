package databreeze.service.etl;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Đọc file người dùng upload. Core hiện hỗ trợ CSV/XLS/XLSX Shopee VN.
 */
public interface FileParsingService {
    /**
     * Parse file thành headers + rows dạng Map<header, value>.
     */
    ParsedFile parse(MultipartFile file) throws IOException;
}
