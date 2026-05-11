package databreeze.dto.etl;

/**
 * Request gợi ý mapping. userAi=true chỉ có tác dụng khi APP_AI_ENABLE=true.
 */
public record SuggestMappingRequest(
        Boolean useAi
) {
    public boolean shoudUseAi() {return Boolean.TRUE.equals( useAi); }
}
