package databreeze.dto.etl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmMappingResponse {
    private UUID importJobId;
    private boolean confirmed;
    private int mappedColumnCount;
    private List<String> missingRequiredFields;
    private String nextStep;
    private String message;
}
