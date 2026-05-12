package databreeze.service.etl;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class FieldNormalizer {

    public String normalize(String input) {
        if (input == null) {
            return "";
        }

        String value = input.trim();

        // Excel header có thể có xuống dòng: "Số\nlượng"
        value = value.replace("\n", " ");
        value = value.replace("\r", " ");
        value = value.replace("\t", " ");

        // Bỏ ký hiệu tiền tệ và ký tự gây nhiễu
        value = value.replace("₫", " ");
        value = value.replace("đ", "d");
        value = value.replace("Đ", "D");

        // Bỏ dấu tiếng Việt
        value = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        value = value.toLowerCase(Locale.ROOT);

        // Đưa mọi ký tự không phải chữ/số thành khoảng trắng
        value = value.replaceAll("[^a-z0-9]+", " ");

        // Gom nhiều khoảng trắng
        value = value.replaceAll("\\s+", " ");

        return value.trim();
    }

    public boolean sameMeaning(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    public boolean containsToken(String source, String token) {
        return normalize(source).contains(normalize(token));
    }
}