package databreeze.service.dev.impl;

import databreeze.dto.dev.SwaggerTestIdsResponse;
import databreeze.entity.Store;
import databreeze.entity.User;
import databreeze.entity.Workspace;
import databreeze.enums.*;
import databreeze.repository.StoreRepository;
import databreeze.repository.UserRepository;
import databreeze.service.dev.DevTestDataService;
import databreeze.service.workspace.WorkspaceBootstrapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Profile("local")
public class DevTestDataServiceImpl implements DevTestDataService {
    private static final String TEST_EMAIL = "seller-demo@databreeze.local";
    private static final String INVITED_EMAIL = "accountant-demo@databreeze.local";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private WorkspaceBootstrapService workspaceBootstrapService;

    @Override
    @Transactional
    public SwaggerTestIdsResponse createOrGetSwaggerTestData() {
        User owner = userRepository.findByEmail(TEST_EMAIL)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(TEST_EMAIL)
                        .fullName("Seller Demo Việt Nam")
                        .authProvider(AuthProvider.EMAIL_PASSWORD)
                        .emailVerified(true)
                        .userType(UserType.BUSINESS)
                        .systemRole(SystemRole.USER)
                        .status(UserStatus.ACTIVE)
                        .build()));

        User invitedUser = userRepository.findByEmail(INVITED_EMAIL)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(INVITED_EMAIL)
                        .fullName("Kế toán Demo Việt Nam")
                        .authProvider(AuthProvider.EMAIL_PASSWORD)
                        .emailVerified(true)
                        .userType(UserType.BUSINESS)
                        .systemRole(SystemRole.USER)
                        .status(UserStatus.ACTIVE)
                        .build()));

        Workspace personalWorkspace = workspaceBootstrapService.getOrCreatePersonalWorkspace(owner);
        Workspace organizationWorkspace = workspaceBootstrapService.getOrCreateOrganizationWorkspace(owner, "DataBreeze Demo Workspace");

        Store personalStore = getOrCreateShopeeStore(personalWorkspace, "Shopee cá nhân demo");
        Store organizationStore = getOrCreateShopeeStore(organizationWorkspace, "Shopee workspace chung demo");

        return SwaggerTestIdsResponse.builder()
                .actorUserId(owner.getId())
                .invitedUserId(invitedUser.getId())
                .personalWorkspaceId(personalWorkspace.getId())
                .organizationWorkspaceId(organizationWorkspace.getId())
                .personalStoreId(personalStore.getId())
                .organizationStoreId(organizationStore.getId())
                .platform(SourcePlatform.SHOPEE.name())
                .dataSourceType(DataSourceType.MARKETPLACE_ORDER.name())
                .ghiChu("Login/JWT chưa setup. Dùng actorUserId để test Swagger. FE cần gọi /api/v1/me/workspaces để hiện list chọn personal/organization workspace.")
                .workspaceSwitcherFlow(List.of(
                        "1. Sau login: GET /api/v1/me/workspaces?actorUserId={actorUserId}",
                        "2. FE hiển thị Personal workspace và Organization workspace trong dropdown/sidebar",
                        "3. User chọn workspace nào thì FE lưu currentWorkspaceId",
                        "4. Tất cả API nghiệp vụ gọi theo currentWorkspaceId: /api/v1/workspaces/{workspaceId}/...",
                        "5. Tạo organization mới: POST /api/v1/organizations?actorUserId={actorUserId}",
                        "6. Personal workspace không có invite; Organization workspace có invite member"
                ))
                .luongTaiKhoanCaNhan(List.of(
                        "1. Context: GET /api/v1/workspaces/{personalWorkspaceId}/context?actorUserId={actorUserId}",
                        "2. Upload: POST /api/v1/workspaces/{personalWorkspaceId}/etl/uploads?actorUserId={actorUserId}&storeId={personalStoreId}",
                        "3. Suggest mapping: POST /api/v1/workspaces/{personalWorkspaceId}/etl/jobs/{importJobId}/suggest-mapping?actorUserId={actorUserId}",
                        "4. Confirm mapping: POST /api/v1/workspaces/{personalWorkspaceId}/etl/jobs/{importJobId}/confirm-mapping?actorUserId={actorUserId}",
                        "5. Run import: POST /api/v1/workspaces/{personalWorkspaceId}/etl/jobs/{importJobId}/run?actorUserId={actorUserId}"
                ))
                .luongWorkspaceChung(List.of(
                        "1. Context: GET /api/v1/workspaces/{organizationWorkspaceId}/context?actorUserId={actorUserId}",
                        "2. Upload: POST /api/v1/workspaces/{organizationWorkspaceId}/etl/uploads?actorUserId={actorUserId}&storeId={organizationStoreId}",
                        "3. Members: GET /api/v1/workspaces/{organizationWorkspaceId}/members?actorUserId={actorUserId}",
                        "4. Invite member: POST /api/v1/workspaces/{organizationWorkspaceId}/members/invitations?actorUserId={actorUserId}",
                        "5. Run ETL giống personal nhưng dùng organizationWorkspaceId"
                ))
                .luongMoiThanhVien(List.of(
                        "1. Owner/Admin gọi invite API với email accountant-demo@databreeze.local",
                        "2. Copy inviteToken từ response",
                        "3. Invited user gọi POST /api/v1/workspaces/invitations/accept?actorUserId={invitedUserId}",
                        "4. Body: { \"token\": \"inviteToken\" }",
                        "5. Sau khi accept, invitedUserId có quyền theo role trong organization workspace"
                ))
                .build();
    }

    private Store getOrCreateShopeeStore(Workspace workspace, String name) {
        return storeRepository.findFirstByWorkspaceIdAndStatus(workspace.getId(), WorkspaceStatus.ACTIVE)
                .orElseGet(() -> storeRepository.save(Store.builder()
                        .workspaceId(workspace.getId())
                        .name(name)
                        .platform(CommercePlatform.SHOPEE)
                        .externalStoreId("LOCAL-DEMO-" + workspace.getWorkspaceType().name())
                        .countryCode("VN")
                        .currencyCode("VND")
                        .status(WorkspaceStatus.ACTIVE)
                        .build()));
    }
}
