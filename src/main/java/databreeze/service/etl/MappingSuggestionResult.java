package databreeze.service.etl;

import databreeze.dto.etl.ColumnMappingDto;
import databreeze.service.ai.AiTokenUsage;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MappingSuggestionResult {
    @Builder.Default
    private List<ColumnMappingDto> mappings = List.of();

    @Builder.Default
    private AiTokenUsage tokenUsage = new AiTokenUsage();

    @Builder.Default
    private boolean aiCalled = false;
}
