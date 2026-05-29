package databreeze.dto.insight;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateInsightResponse {
    private int generatedCount;
    private String message;
    private List<BusinessInsightResponse> insights;
}
