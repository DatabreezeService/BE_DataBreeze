package databreeze.api;

import databreeze.dto.analytics.ProcessedCommerceDataResponse;
import databreeze.security.CurrentUser;
import databreeze.security.UserPrincipal;
import databreeze.service.analytics.ProcessedDataQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/processed-data")
@Tag(name = "Processed Commerce Data", description = "API lay du lieu orders/order_items/products sau ETL.")
@SecurityRequirement(name = "bearer")
public class ProcessedDataController {

    @Autowired
    private ProcessedDataQueryService processedDataQueryService;

    @GetMapping("/shopee")
    @Operation(summary = "Lay du lieu Shopee da xu ly kem thong ke chi tiet")
    public ProcessedCommerceDataResponse shopeeProcessedData(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return processedDataQueryService.getShopeeProcessedData(
                workspaceId,
                CurrentUser.requireUserId(principal),
                storeId,
                fromDate,
                toDate
        );
    }
}
