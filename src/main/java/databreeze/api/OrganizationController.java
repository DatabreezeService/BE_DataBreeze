package databreeze.api;

import databreeze.dto.workspace.CreateOrganizationWorkspaceRequest;
import databreeze.dto.workspace.CreateOrganizationWorkspaceResponse;
import databreeze.service.workspace.WorkspaceCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organization Workspace", description = "API tạo organization workspace. Login/JWT setup sau, hiện dùng actorUserId để test Swagger.")
public class OrganizationController {

    @Autowired
    private WorkspaceCommandService workspaceCommandService;

    @PostMapping
    @Operation(
            summary = "Tạo organization workspace mới",
            description = "Owner là actorUserId. Sau khi tạo, FE refresh GET /api/v1/me/workspaces để hiển thị workspace mới trong list."
    )
    public CreateOrganizationWorkspaceResponse createOrganizationWorkspace(@RequestParam UUID actorUserId,
                                                                          @Valid @RequestBody CreateOrganizationWorkspaceRequest request) {
        return workspaceCommandService.createOrganizationWorkspace(actorUserId, request);
    }
}
