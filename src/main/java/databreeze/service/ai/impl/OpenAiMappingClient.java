package databreeze.service.ai.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import databreeze.dto.etl.ColumnMappingDto;
import databreeze.entity.TargetSchemaField;
import databreeze.enums.MappingSource;
import databreeze.enums.TargetDataType;
import databreeze.service.ai.AiMappingClient;
import databreeze.service.etl.ParsedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * OpenAI-compatible mapping client.
 * Chỉ gửi metadata: headers + sample rows + target fields. Không gửi toàn bộ file.
 */
@Component
public class OpenAiMappingClient implements AiMappingClient {
    @Autowired
    private ObjectMapper mapper;

    private HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.ai.enabled:false}")
    private boolean enabled;

    @Value("${app.ai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.model:gpt-4.1-mini}")
    private String model;

    @Override
    public List<ColumnMappingDto> suggestMappings(ParsedFile file, List<TargetSchemaField> targetFields) {
        if (!enabled || apiKey == null || apiKey.isBlank()) return List.of();
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("temperature", 0.1);
            payload.put("response_format", Map.of("type", "json_object"));
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt()),
                    Map.of("role", "user", "content", userPrompt(file, targetFields))
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl.replaceAll("/$", "") + "/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return List.of();

            JsonNode root = mapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText();
            JsonNode contentNode = mapper.readTree(content);
            List<AiMap> aiMaps = mapper.convertValue(contentNode.path("mappings"), new TypeReference<List<AiMap>>() {});
            Map<String, TargetSchemaField> fieldMap = new HashMap<>();
            for (TargetSchemaField field : targetFields) fieldMap.put(field.getFieldName(), field);

            return aiMaps.stream()
                    .filter(item -> item.sourceColumnName != null && item.targetFieldName != null && fieldMap.containsKey(item.targetFieldName))
                    .map(item -> {
                        TargetSchemaField field = fieldMap.get(item.targetFieldName);
                        return new ColumnMappingDto(
                                field.getId(),
                                item.sourceColumnName,
                                field.getFieldName(),
                                field.getDisplayName(),
                                item.targetDataType == null ? field.getDataType() : item.targetDataType,
                                Boolean.TRUE.equals(field.getIsRequired()),
                                item.confidenceScore == null ? BigDecimal.valueOf(0.75) : item.confidenceScore,
                                item.reason == null ? "AI gợi ý mapping theo ngữ nghĩa cột" : item.reason,
                                item.transformRule == null ? Map.of("source", MappingSource.AI.name()) : item.transformRule,
                                false
                        );
                    })
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String systemPrompt() {
        return "Bạn là bộ máy mapping dữ liệu ETL cho thị trường Việt Nam, ưu tiên Shopee Việt Nam. " +
                "Nhiệm vụ: ánh xạ cột Excel/CSV tiếng Việt hoặc tiếng Anh bị viết tắt sang field chuẩn trong database. " +
                "Chỉ trả về JSON hợp lệ với key mappings. Không bịa source column. Không tự import dữ liệu. " +
                "Chỉ confidence cao khi mapping thật sự rõ ràng.";
    }

    private String userPrompt(ParsedFile file, List<TargetSchemaField> targetFields) throws Exception {
        Map<String, Object> prompt = new LinkedHashMap<>();
        prompt.put("nguon_du_lieu", "SHOPEE_ORDER_VN");
        prompt.put("cot_nguon_trong_file", file.getHeaders());
        prompt.put("du_lieu_mau_toi_da_5_dong", file.getRows().stream().limit(5).toList());
        prompt.put("field_chuan_trong_database", targetFields.stream().map(field -> Map.of(
                "ten_field_chuan", field.getFieldName(),
                "ten_hien_thi_tieng_viet", field.getDisplayName(),
                "kieu_du_lieu", field.getDataType().name(),
                "bat_buoc", field.getIsRequired(),
                "mo_ta", Optional.ofNullable(field.getDescription()).orElse("")
        )).toList());
        prompt.put("dinh_dang_json_bat_buoc", Map.of("mappings", List.of(Map.of(
                "sourceColumnName", "Tên cột trong file",
                "targetFieldName", "field_chuan_trong_db",
                "targetDataType", "STRING|INTEGER|DECIMAL|DATE|DATETIME|BOOLEAN|CURRENCY|JSON",
                "confidenceScore", 0.91,
                "reason", "Lý do mapping ngắn gọn bằng tiếng Việt",
                "transformRule", Map.of("trim", true)
        ))));
        return mapper.writeValueAsString(prompt);
    }

    public static class AiMap {
        public String sourceColumnName;
        public String targetFieldName;
        public TargetDataType targetDataType;
        public BigDecimal confidenceScore;
        public String reason;
        public Map<String, Object> transformRule;
    }
}
