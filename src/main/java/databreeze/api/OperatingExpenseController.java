package databreeze.api;

import databreeze.dto.expense.OperatingExpenseRequest;
import databreeze.dto.expense.OperatingExpenseResponse;
import databreeze.security.CurrentUser;
import databreeze.security.UserPrincipal;
import databreeze.service.expense.OperatingExpenseService;
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
@RequestMapping("/api/v1/workspaces/{workspaceId}/operating-expenses")
@Tag(name = "Operating Expenses", description = "Quan ly chi phi van hanh de tinh loi nhuan rong.")
@SecurityRequirement(name = "bearer")
public class OperatingExpenseController {

    @Autowired
    private OperatingExpenseService operatingExpenseService;

    @GetMapping
    @Operation(summary = "Xem danh sach chi phi van hanh")
    public List<OperatingExpenseResponse> listExpenses(@PathVariable UUID workspaceId,
                                                       @AuthenticationPrincipal UserPrincipal principal,
                                                       @RequestParam(required = false) UUID storeId,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return operatingExpenseService.listExpenses(workspaceId, CurrentUser.requireUserId(principal), storeId, fromDate, toDate);
    }

    @PostMapping
    @Operation(summary = "Tao chi phi van hanh")
    public OperatingExpenseResponse createExpense(@PathVariable UUID workspaceId,
                                                  @AuthenticationPrincipal UserPrincipal principal,
                                                  @Valid @RequestBody OperatingExpenseRequest request) {
        return operatingExpenseService.createExpense(workspaceId, CurrentUser.requireUserId(principal), request);
    }

    @PatchMapping("/{expenseId}")
    @Operation(summary = "Cap nhat chi phi van hanh")
    public OperatingExpenseResponse updateExpense(@PathVariable UUID workspaceId,
                                                  @PathVariable UUID expenseId,
                                                  @AuthenticationPrincipal UserPrincipal principal,
                                                  @Valid @RequestBody OperatingExpenseRequest request) {
        return operatingExpenseService.updateExpense(workspaceId, CurrentUser.requireUserId(principal), expenseId, request);
    }

    @DeleteMapping("/{expenseId}")
    @Operation(summary = "Xoa chi phi van hanh")
    public void deleteExpense(@PathVariable UUID workspaceId,
                              @PathVariable UUID expenseId,
                              @AuthenticationPrincipal UserPrincipal principal) {
        operatingExpenseService.deleteExpense(workspaceId, CurrentUser.requireUserId(principal), expenseId);
    }
}
