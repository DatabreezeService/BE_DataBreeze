package databreeze.dto.audit;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditLogPageResponse {
    private List<AuditLogResponse> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
