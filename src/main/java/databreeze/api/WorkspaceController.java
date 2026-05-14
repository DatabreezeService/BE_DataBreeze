package databreeze.api;

import databreeze.dto.workspace.AcceptInvitationRequest;
import databreeze.dto.workspace.AcceptInvitationResponse;
import databreeze.dto.workspace.InviteMemberRequest;
import databreeze.dto.workspace.InviteMemberResponse;
import databreeze.dto.workspace.WorkspaceContextDto;
import databreeze.dto.workspace.WorkspaceMemberResponse;
import databreeze.security.CurrentUser;
import databreeze.security.UserPrincipal;
import databreeze.service.workspace.WorkspaceAccessService;
import databreeze.service.workspace.WorkspaceMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
@Tag(name = "Workspace", description = "API dung chung cho personal workspace va organization workspace.")
@SecurityRequirement(name = "bearer")
public class WorkspaceController {

    @Autowired
    private WorkspaceAccessService workspaceAccessService;

    @Autowired
    private WorkspaceMemberService workspaceMemberService;

    @GetMapping("/{workspaceId}/context")
    @Operation(summary = "Xem context quyen cua user trong workspace")
    public WorkspaceContextDto context(@PathVariable UUID workspaceId,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return workspaceAccessService.describeContext(workspaceId, CurrentUser.requireUserId(principal));
    }

    @PostMapping("/{workspaceId}/members/invitations")
    @Operation(summary = "Owner/Admin moi thanh vien vao organization workspace")
    public InviteMemberResponse inviteMember(@PathVariable UUID workspaceId,
                                             @AuthenticationPrincipal UserPrincipal principal,
                                             @Valid @RequestBody InviteMemberRequest request) {
        return workspaceMemberService.inviteMember(workspaceId, CurrentUser.requireUserId(principal), request);
    }

    @PostMapping("/invitations/accept")
    @Operation(summary = "User accept loi moi vao workspace")
    public AcceptInvitationResponse acceptInvitation(@AuthenticationPrincipal UserPrincipal principal,
                                                     @Valid @RequestBody AcceptInvitationRequest request) {
        return workspaceMemberService.acceptInvitation(CurrentUser.requireUserId(principal), request);
    }

    @GetMapping("/{workspaceId}/members")
    @Operation(summary = "Danh sach member active trong workspace")
    public List<WorkspaceMemberResponse> members(@PathVariable UUID workspaceId,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return workspaceMemberService.listActiveMembers(workspaceId, CurrentUser.requireUserId(principal));
    }
}
