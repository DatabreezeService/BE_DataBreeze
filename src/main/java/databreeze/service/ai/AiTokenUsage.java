package databreeze.service.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiTokenUsage {
    @Builder.Default
    private long inputTokens = 0L;

    @Builder.Default
    private long outputTokens = 0L;

    @Builder.Default
    private long totalTokens = 0L;

    public boolean hasUsage() {
        return totalTokens > 0 || inputTokens > 0 || outputTokens > 0;
    }
}
