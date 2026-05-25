package databreeze.service.ai;

import databreeze.dto.etl.ColumnMappingDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiMappingResult {
    @Builder.Default
    private List<ColumnMappingDto> mappings = List.of();

    @Builder.Default
    private AiTokenUsage tokenUsage = new AiTokenUsage();

    @Builder.Default
    private boolean aiCalled = false;

    public static AiMappingResult empty() {
        return AiMappingResult.builder().build();
    }
}
