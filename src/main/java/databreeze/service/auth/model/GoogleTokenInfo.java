package databreeze.service.auth.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoogleTokenInfo {
    private String subject;
    private String email;
    private String name;
    private String picture;
    private boolean emailVerified;
}
