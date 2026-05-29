package databreeze.api.store;

import databreeze.dto.store.CreateStoreRequest;
import databreeze.dto.store.StoreResponse;
import databreeze.dto.store.UpdateStoreRequest;
import databreeze.security.CurrentUser;
import databreeze.security.UserPrincipal;
import databreeze.service.store.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/stores")
@Tag(name = "Stores", description = "Quản lý shop/store trong workspace để upload, import và generate insight.")
@SecurityRequirement(name = "bearer")
public class StoreController {
    @Autowired
    private StoreService storeService;

    @GetMapping
    @Operation(summary = "Danh sách store trong workspace")
    public List<StoreResponse> listStores(@PathVariable UUID workspaceId,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return storeService.listStores(workspaceId, CurrentUser.requireUserId(principal));
    }

    @PostMapping
    @Operation(summary = "Tạo store/shop mới")
    public StoreResponse createStore(@PathVariable UUID workspaceId,
                                     @AuthenticationPrincipal UserPrincipal principal,
                                     @Valid @RequestBody CreateStoreRequest request) {
        return storeService.createStore(workspaceId, CurrentUser.requireUserId(principal), request);
    }

    @GetMapping("/{storeId}")
    @Operation(summary = "Xem chi tiết store")
    public StoreResponse getStore(@PathVariable UUID workspaceId,
                                  @PathVariable UUID storeId,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        return storeService.getStore(workspaceId, CurrentUser.requireUserId(principal), storeId);
    }

    @PatchMapping("/{storeId}")
    @Operation(summary = "Cập nhật store")
    public StoreResponse updateStore(@PathVariable UUID workspaceId,
                                     @PathVariable UUID storeId,
                                     @AuthenticationPrincipal UserPrincipal principal,
                                     @Valid @RequestBody UpdateStoreRequest request) {
        return storeService.updateStore(workspaceId, CurrentUser.requireUserId(principal), storeId, request);
    }

    @DeleteMapping("/{storeId}")
    @Operation(summary = "Archive store")
    public StoreResponse archiveStore(@PathVariable UUID workspaceId,
                                      @PathVariable UUID storeId,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return storeService.archiveStore(workspaceId, CurrentUser.requireUserId(principal), storeId);
    }
}
