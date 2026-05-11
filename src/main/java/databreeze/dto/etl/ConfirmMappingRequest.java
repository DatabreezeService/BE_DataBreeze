package databreeze.dto.etl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ConfirmMappingRequest (
        @NotEmpty(message = "Danh sách mapping không được để trống.")
        List<@Valid ColumnMappingDto> mappings
) {}
