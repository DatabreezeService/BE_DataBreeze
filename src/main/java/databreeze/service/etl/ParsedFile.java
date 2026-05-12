package databreeze.service.etl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsedFile {
    private List<String> headers;
    private List<Map<String, Object>> rows;
}
