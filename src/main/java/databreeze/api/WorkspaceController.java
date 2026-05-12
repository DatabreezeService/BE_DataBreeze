package databreeze.api;

import databreeze.dto.workspace.*;
import databreeze.service.workspace.WorkspaceAccessService;
import databreeze.service.workspace.WorkspaceMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
@Tag(name = "Workspace", description = "API dùng chung cho personal workspace và organization workspace.")
public class WorkspaceController {

    @Autowired
    private WorkspaceAccessService workspaceAccessService;

    @Autowired
    private WorkspaceMemberService workspaceMemberService;

    @GetMapping("/{workspaceId}/context")
    @Operation(summary = "Xem context quyền của user trong workspace", description = "FE dùng API này để biết workspace là PERSONAL/ORGANIZATION và user có permission nào.")
    public WorkspaceContextDto context(@PathVariable UUID workspaceId,
                                       @RequestParam UUID actorUserId) {
        return workspaceAccessService.describeContext(workspaceId, actorUserId);
    }

    @PostMapping("/{workspaceId}/members/invitations")
    @Operation(summary = "Owner/Admin mời thành viên vào organization workspace", description = "MVP local trả inviteToken trực tiếp. Sau này thay bằng gửi email.")
    public InviteMemberResponse inviteMember(@PathVariable UUID workspaceId,
                                             @RequestParam UUID actorUserId,
                                             @Valid @RequestBody InviteMemberRequest request) {
        return workspaceMemberService.inviteMember(workspaceId, actorUserId, request);
    }

    @PostMapping("/invitations/accept")
    @Operation(summary = "User accept lời mời vào workspace", description = "actorUserId là user đang đăng nhập. Email user phải khớp invitedEmail trong invitation.")
    public AcceptInvitationResponse acceptInvitation(@RequestParam UUID actorUserId,
                                                     @Valid @RequestBody AcceptInvitationRequest request) {
        return workspaceMemberService.acceptInvitation(actorUserId, request);
    }

    @GetMapping("/{workspaceId}/members")
    @Operation(summary = "Danh sách member active trong workspace")
    public List<WorkspaceMemberResponse> members(@PathVariable UUID workspaceId,
                                                 @RequestParam UUID actorUserId) {
        return workspaceMemberService.listActiveMembers(workspaceId, actorUserId);
    }
}
