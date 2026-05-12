package databreeze.dto.etl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunImportRequest {
    private Boolean skipInvalidRows;

    public boolean shouldSkipInvalidRows() {
        return !Boolean.FALSE.equals(skipInvalidRows);
    }
}
