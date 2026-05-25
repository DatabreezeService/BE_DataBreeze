package databreeze.dto.billing;

import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageResponse {
    private UUID workspaceId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Long uploadCount;
    private Long uploadLimit;
    private Long importedRowCount;
    private Long importedRowLimit;
    private Long aiMappingCount;
    private Long insightGenerationCount;
    private Long insightGenerationLimit;
    private Long aiInputTokens;
    private Long aiOutputTokens;
    private Long aiTotalTokens;
    private Long aiTokenLimit;
    private Long aiTokenRemaining;
}
