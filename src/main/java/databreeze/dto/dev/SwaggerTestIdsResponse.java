package databreeze.dto.dev;

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
public class SwaggerTestIdsResponse {
    private UUID actorUserId;
    private UUID invitedUserId;
    private UUID personalWorkspaceId;
    private UUID organizationWorkspaceId;
    private UUID personalStoreId;
    private UUID organizationStoreId;
    private String platform;
    private String dataSourceType;
    private String ghiChu;
    private List<String> workspaceSwitcherFlow;
    private List<String> luongTaiKhoanCaNhan;
    private List<String> luongWorkspaceChung;
    private List<String> luongMoiThanhVien;
}
