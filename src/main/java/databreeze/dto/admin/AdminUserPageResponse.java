package databreeze.dto.admin;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserPageResponse {
    private List<AdminUserResponse> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
