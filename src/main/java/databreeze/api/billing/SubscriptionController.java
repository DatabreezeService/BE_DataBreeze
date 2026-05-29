package databreeze.api.billing;

import databreeze.dto.billing.SubscribeRequest;
import databreeze.dto.billing.SubscriptionResponse;
import databreeze.dto.billing.UsageResponse;
import databreeze.security.CurrentUser;
import databreeze.security.UserPrincipal;
import databreeze.service.billing.SubscriptionService;
import databreeze.service.billing.UsageMeterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}")
@Tag(name = "Workspace Billing", description = "Subscription, token usage và quota theo workspace.")
@SecurityRequirement(name = "bearer")
public class SubscriptionController {
    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UsageMeterService usageMeterService;

    @GetMapping("/subscription")
    @Operation(summary = "Xem subscription hiện tại")
    public SubscriptionResponse currentSubscription(@PathVariable UUID workspaceId,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return usageMeterService.getSubscription(workspaceId, CurrentUser.requireUserId(principal));
    }

    @PostMapping("/subscription")
    @Operation(summary = "Đổi hoặc kích hoạt gói subscription")
    public SubscriptionResponse subscribe(@PathVariable UUID workspaceId,
                                          @AuthenticationPrincipal UserPrincipal principal,
                                          @Valid @RequestBody SubscribeRequest request) {
        return subscriptionService.subscribe(workspaceId, CurrentUser.requireUserId(principal), request.getPlanCode());
    }

    @GetMapping("/usage")
    @Operation(summary = "Xem usage và token AI còn lại")
    public UsageResponse usage(@PathVariable UUID workspaceId,
                               @AuthenticationPrincipal UserPrincipal principal) {
        return usageMeterService.getUsage(workspaceId, CurrentUser.requireUserId(principal));
    }
}
