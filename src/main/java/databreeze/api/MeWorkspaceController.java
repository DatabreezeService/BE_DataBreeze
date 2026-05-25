package databreeze.api;

import databreeze.dto.workspace.InitPersonalWorkspaceResponse;
import databreeze.dto.workspace.WorkspaceSwitcherResponse;
import databreeze.service.workspace.WorkspaceCommandService;
import databreeze.service.workspace.WorkspaceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Me / Workspace Switcher", description = "API để FE lấy danh sách workspace sau login. MVP chưa có JWT nên tạm truyền actorUserId.")
public class MeWorkspaceController {

    @Autowired
    private WorkspaceQueryService workspaceQueryService;

    @Autowired
    private WorkspaceCommandService workspaceCommandService;

    @GetMapping("/workspaces")
    @Operation(
            summary = "Danh sách workspace mà user có thể chọn",
            description = "FE gọi API này sau login để hiển thị workspace switcher giống ChatGPT: personal workspace + organization workspaces."
    )
    public WorkspaceSwitcherResponse myWorkspaces(@RequestParam UUID actorUserId) {
        return workspaceQueryService.listMyWorkspaces(actorUserId);
    }

    @PostMapping("/personal-workspace/init")
    @Operation(
            summary = "Khởi tạo/lấy personal workspace mặc định",
            description = "Tạm dùng để test khi chưa có auth. Sau này auth service gọi tự động sau đăng ký/login lần đầu."
    )
    public InitPersonalWorkspaceResponse initPersonalWorkspace(@RequestParam UUID actorUserId) {
        return workspaceCommandService.initPersonalWorkspace(actorUserId);
    }
}
