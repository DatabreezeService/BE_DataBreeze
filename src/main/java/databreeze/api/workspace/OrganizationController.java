package databreeze.api.workspace;

import databreeze.dto.workspace.CreateOrganizationWorkspaceRequest;
import databreeze.dto.workspace.CreateOrganizationWorkspaceResponse;
import databreeze.security.CurrentUser;
import databreeze.security.UserPrincipal;
import databreeze.service.workspace.WorkspaceCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organization Workspace", description = "API tao organization workspace.")
@SecurityRequirement(name = "bearer")
public class OrganizationController {

    @Autowired
    private WorkspaceCommandService workspaceCommandService;

    @PostMapping
    @Operation(summary = "Tao organization workspace moi")
    public CreateOrganizationWorkspaceResponse createOrganizationWorkspace(@AuthenticationPrincipal UserPrincipal principal,
                                                                          @Valid @RequestBody CreateOrganizationWorkspaceRequest request) {
        return workspaceCommandService.createOrganizationWorkspace(CurrentUser.requireUserId(principal), request);
    }
}
