package databreeze.dto.etl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestMappingRequest {
    private Boolean useAi;

    public boolean shouldUseAi() {
        return Boolean.TRUE.equals(useAi);
    }
}
