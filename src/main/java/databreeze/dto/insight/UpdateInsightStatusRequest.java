package databreeze.dto.insight;

import databreeze.enums.InsightStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInsightStatusRequest {
    @NotNull(message = "Trạng thái insight không được để trống.")
    private InsightStatus status;
}
