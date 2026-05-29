package databreeze.service.ai;

import databreeze.entity.TargetSchemaField;
import databreeze.service.etl.ParsedFile;

import java.util.List;

/**
 * Client gọi AI mapping. Interface để sau này đổi OpenAI/Gemini/OpenRouter mà không sửa service chính.
 */
public interface AiMappingClient {
    AiMappingResult suggestMappings(ParsedFile file, List<TargetSchemaField> targetFields);
}
