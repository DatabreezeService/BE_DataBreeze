package databreeze.api;

import databreeze.dto.cost.ApplyProductCostRequest;
import databreeze.dto.cost.ApplyProductCostResponse;
import databreeze.dto.cost.BulkProductCostRequest;
import databreeze.dto.cost.MissingCostSkuResponse;
import databreeze.dto.cost.ProductCostRequest;
import databreeze.dto.cost.ProductCostResponse;
import databreeze.security.CurrentUser;
import databreeze.security.UserPrincipal;
import databreeze.service.cost.ProductCostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/product-costs")
@Tag(name = "Product Costs", description = "Quan ly gia von SKU va ap COGS vao du lieu da import.")
@SecurityRequirement(name = "bearer")
public class ProductCostController {

    @Autowired
    private ProductCostService productCostService;

    @GetMapping
    @Operation(summary = "Xem danh sach gia von SKU")
    public List<ProductCostResponse> listCosts(@PathVariable UUID workspaceId,
                                               @AuthenticationPrincipal UserPrincipal principal,
                                               @RequestParam(required = false) String sku) {
        return productCostService.listCosts(workspaceId, CurrentUser.requireUserId(principal), sku);
    }

    @PostMapping
    @Operation(summary = "Tao gia von cho SKU")
    public ProductCostResponse createCost(@PathVariable UUID workspaceId,
                                          @AuthenticationPrincipal UserPrincipal principal,
                                          @Valid @RequestBody ProductCostRequest request) {
        return productCostService.createCost(workspaceId, CurrentUser.requireUserId(principal), request);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Tao nhieu gia von SKU va tuy chon ap vao orders da import")
    public List<ProductCostResponse> bulkCreateCosts(@PathVariable UUID workspaceId,
                                                     @AuthenticationPrincipal UserPrincipal principal,
                                                     @Valid @RequestBody BulkProductCostRequest request) {
        return productCostService.bulkCreateCosts(workspaceId, CurrentUser.requireUserId(principal), request);
    }

    @PatchMapping("/{costId}")
    @Operation(summary = "Cap nhat gia von SKU")
    public ProductCostResponse updateCost(@PathVariable UUID workspaceId,
                                          @PathVariable UUID costId,
                                          @AuthenticationPrincipal UserPrincipal principal,
                                          @Valid @RequestBody ProductCostRequest request) {
        return productCostService.updateCost(workspaceId, CurrentUser.requireUserId(principal), costId, request);
    }

    @DeleteMapping("/{costId}")
    @Operation(summary = "Xoa gia von SKU")
    public void deleteCost(@PathVariable UUID workspaceId,
                           @PathVariable UUID costId,
                           @AuthenticationPrincipal UserPrincipal principal) {
        productCostService.deleteCost(workspaceId, CurrentUser.requireUserId(principal), costId);
    }

    @PostMapping("/apply")
    @Operation(summary = "Ap gia von vao order_items va tinh lai dashboard")
    public ApplyProductCostResponse applyCosts(@PathVariable UUID workspaceId,
                                               @AuthenticationPrincipal UserPrincipal principal,
                                               @RequestBody(required = false) ApplyProductCostRequest request) {
        return productCostService.applyCosts(workspaceId, CurrentUser.requireUserId(principal), request);
    }

    @GetMapping("/missing-skus")
    @Operation(summary = "Lay danh sach SKU dang thieu gia von trong data da import")
    public List<MissingCostSkuResponse> missingSkus(@PathVariable UUID workspaceId,
                                                    @AuthenticationPrincipal UserPrincipal principal,
                                                    @RequestParam(required = false) UUID storeId,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return productCostService.listMissingCostSkus(workspaceId, CurrentUser.requireUserId(principal), storeId, fromDate, toDate);
    }
}
