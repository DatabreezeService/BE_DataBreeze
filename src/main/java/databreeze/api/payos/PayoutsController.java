package databreeze.api.payos;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import databreeze.dto.common.ApiResponse;
import vn.payos.PayOS;
import vn.payos.model.v1.payouts.GetPayoutListParams;
import vn.payos.model.v1.payouts.GetPayoutListParams.GetPayoutListParamsBuilder;
import vn.payos.model.v1.payouts.Payout;
import vn.payos.model.v1.payouts.PayoutApprovalState;
import vn.payos.model.v1.payouts.PayoutRequests;
import vn.payos.model.v1.payouts.batch.PayoutBatchItem;
import vn.payos.model.v1.payouts.batch.PayoutBatchRequest;
import vn.payos.model.v1.payoutsAccount.PayoutAccountInfo;

@RestController
@RequestMapping("/api/v1/payos/payouts")
public class PayoutsController {
  private final PayOS payOS;

  public PayoutsController(PayOS payOSPayout) {
    super();
    this.payOS = payOSPayout;
  }

  @PostMapping("/create")
  public ApiResponse<Payout> create(@RequestBody PayoutRequests body) throws Exception {
    if (body.getReferenceId() == null || body.getReferenceId().isEmpty()) {
      body.setReferenceId("payout_" + (System.currentTimeMillis() / 1000));
    }

    Payout payout = payOS.payouts().create(body);
    return ApiResponse.success(payout);
  }

  @PostMapping("/batch/create")
  public ApiResponse<Payout> createBatch(@RequestBody PayoutBatchRequest body) throws Exception {
    if (body.getReferenceId() == null || body.getReferenceId().isEmpty()) {
      body.setReferenceId("payout_" + (System.currentTimeMillis() / 1000));
    }

    List<PayoutBatchItem> payoutsList = body.getPayouts();
    if (payoutsList == null) {
      throw new IllegalArgumentException("payouts khong duoc null");
    }

    for (int i = 0; i < payoutsList.size(); i++) {
      PayoutBatchItem batchItem = payoutsList.get(i);
      if (batchItem.getReferenceId() == null) {
        batchItem.setReferenceId("payout_" + (System.currentTimeMillis() / 1000) + "_" + i);
      }
    }

    Payout payout = payOS.payouts().batch().create(body);
    return ApiResponse.success(payout);
  }

  @GetMapping("/{payoutId}")
  public ApiResponse<Payout> retrieve(@PathVariable String payoutId) throws Exception {
    Payout payout = payOS.payouts().get(payoutId);
    return ApiResponse.success(payout);
  }

  @GetMapping("/list")
  public ApiResponse<List<Payout>> retrieveList(
      @RequestParam(required = false) String referenceId,
      @RequestParam(required = false) String approvalState,
      @RequestParam(required = false) List<String> category,
      @RequestParam(required = false) String fromDate,
      @RequestParam(required = false) String toDate,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) Integer offset) throws Exception {
    GetPayoutListParamsBuilder paramsBuilder =
        GetPayoutListParams.builder().referenceId(referenceId).category(category).limit(limit).offset(offset);
    if (fromDate != null && !fromDate.isEmpty()) {
      paramsBuilder.fromDate(fromDate);
    }
    if (toDate != null && !toDate.isEmpty()) {
      paramsBuilder.toDate(toDate);
    }

    if (approvalState != null && !approvalState.isEmpty()) {
      try {
        paramsBuilder.approvalState(PayoutApprovalState.valueOf(approvalState.toUpperCase(Locale.ROOT)));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid approval state: " + approvalState);
      }
    }

    GetPayoutListParams params = paramsBuilder.build();

    List<Payout> data = new ArrayList<>();
    payOS.payouts().list(params).autoPager().stream().forEach(data::add);
    return ApiResponse.success(data);
  }

  @GetMapping("/balance")
  public ApiResponse<PayoutAccountInfo> getAccountBalance() throws Exception {
    PayoutAccountInfo accountInfo = payOS.payoutsAccount().balance();
    return ApiResponse.success(accountInfo);
  }
}
