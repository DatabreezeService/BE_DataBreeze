package databreeze.api.workspace;

import databreeze.dto.workspace.InitPersonalWorkspaceResponse;
import databreeze.dto.workspace.WorkspaceSwitcherResponse;
import databreeze.security.CurrentUser;
import databreeze.security.UserPrincipal;
import databreeze.service.workspace.WorkspaceCommandService;
import databreeze.service.workspace.WorkspaceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Me / Workspace Switcher", description = "API lay workspace cua user dang dang nhap.")
@SecurityRequirement(name = "bearer")
public class MeWorkspaceController {

    @Autowired
    private WorkspaceQueryService workspaceQueryService;

    @Autowired
    private WorkspaceCommandService workspaceCommandService;

    @GetMapping("/workspaces")
    @Operation(summary = "Danh sach workspace ma user co the chon")
    public WorkspaceSwitcherResponse myWorkspaces(@AuthenticationPrincipal UserPrincipal principal) {
        return workspaceQueryService.listMyWorkspaces(CurrentUser.requireUserId(principal));
    }

    @PostMapping("/personal-workspace/init")
    @Operation(summary = "Khoi tao/lay personal workspace mac dinh")
    public InitPersonalWorkspaceResponse initPersonalWorkspace(@AuthenticationPrincipal UserPrincipal principal) {
        return workspaceCommandService.initPersonalWorkspace(CurrentUser.requireUserId(principal));
    }
}
