package databreeze.dto.etl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ConfirmMappingRequest {
    @NotEmpty(message = "Danh sách mapping không được để trống.")
    private List<@Valid ColumnMappingDto> mappings;
}
