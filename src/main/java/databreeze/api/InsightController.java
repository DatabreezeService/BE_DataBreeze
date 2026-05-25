package databreeze.api;

import databreeze.dto.insight.BusinessInsightResponse;
import databreeze.dto.insight.GenerateInsightRequest;
import databreeze.dto.insight.GenerateInsightResponse;
import databreeze.dto.insight.UpdateInsightStatusRequest;
import databreeze.enums.InsightStatus;
import databreeze.security.CurrentUser;
import databreeze.security.UserPrincipal;
import databreeze.service.insight.BusinessInsightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/insights")
@Tag(name = "Business Insights", description = "Tạo và xem insight từ dữ liệu đã ETL.")
@SecurityRequirement(name = "bearer")
public class InsightController {
    @Autowired
    private BusinessInsightService businessInsightService;

    @PostMapping("/generate")
    @Operation(summary = "Tạo insight Shopee từ dữ liệu đã xử lý")
    public GenerateInsightResponse generate(@PathVariable UUID workspaceId,
                                            @AuthenticationPrincipal UserPrincipal principal,
                                            @Valid @RequestBody(required = false) GenerateInsightRequest request) {
        return businessInsightService.generate(workspaceId, CurrentUser.requireUserId(principal), request);
    }

    @GetMapping
    @Operation(summary = "Xem danh sách insight")
    public List<BusinessInsightResponse> list(@PathVariable UUID workspaceId,
                                              @AuthenticationPrincipal UserPrincipal principal,
                                              @RequestParam(required = false) InsightStatus status,
                                              @RequestParam(required = false) UUID storeId,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return businessInsightService.list(workspaceId, CurrentUser.requireUserId(principal), status, storeId, fromDate, toDate);
    }

    @PatchMapping("/{insightId}/status")
    @Operation(summary = "Cập nhật trạng thái insight")
    public BusinessInsightResponse updateStatus(@PathVariable UUID workspaceId,
                                                @PathVariable UUID insightId,
                                                @AuthenticationPrincipal UserPrincipal principal,
                                                @Valid @RequestBody UpdateInsightStatusRequest request) {
        return businessInsightService.updateStatus(workspaceId, CurrentUser.requireUserId(principal), insightId, request.getStatus());
    }
}
