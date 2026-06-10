package databreeze.api.payos;

import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import databreeze.dto.common.ApiResponse;
import databreeze.dto.payments.CreatePaymentLinkRequestBody;
import vn.payos.PayOS;
import vn.payos.core.FileDownloadResponse;
import vn.payos.exception.APIException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.v2.paymentRequests.invoices.InvoicesInfo;
import vn.payos.model.webhooks.ConfirmWebhookResponse;

@RestController
@RequestMapping("/api/v1/payos/order")
public class OrderController {
  private final PayOS payOS;

  public OrderController(PayOS payOS) {
    super();
    this.payOS = payOS;
  }

  @PostMapping(path = "/create")
  public ApiResponse<CreatePaymentLinkResponse> createPaymentLink(
      @Valid @RequestBody CreatePaymentLinkRequestBody requestBody) throws Exception {
    long orderCode =
        requestBody.getOrderCode() == null ? System.currentTimeMillis() / 1000 : requestBody.getOrderCode();
    long price = requestBody.getPrice();
    int quantity = requestBody.getQuantity() == null ? 1 : requestBody.getQuantity();

    PaymentLinkItem item =
        PaymentLinkItem.builder().name(requestBody.getProductName()).quantity(quantity).price(price).build();

    CreatePaymentLinkRequest paymentData =
        CreatePaymentLinkRequest.builder()
            .orderCode(orderCode)
            .description(requestBody.getDescription())
            .amount(price)
            .item(item)
            .returnUrl(requestBody.getReturnUrl())
            .cancelUrl(requestBody.getCancelUrl())
            .build();

    CreatePaymentLinkResponse data = payOS.paymentRequests().create(paymentData);
    return ApiResponse.success(data);
  }

  @GetMapping(path = "/{orderId}")
  public ApiResponse<PaymentLink> getOrderById(@PathVariable("orderId") long orderId) throws Exception {
    PaymentLink order = payOS.paymentRequests().get(orderId);
    return ApiResponse.success("ok", order);
  }

  @PutMapping(path = "/{orderId}")
  public ApiResponse<PaymentLink> cancelOrder(@PathVariable("orderId") long orderId) throws Exception {
    PaymentLink order = payOS.paymentRequests().cancel(orderId, "change my mind");
    return ApiResponse.success("ok", order);
  }

  @PostMapping(path = "/confirm-webhook")
  public ApiResponse<ConfirmWebhookResponse> confirmWebhook(
      @RequestBody Map<String, String> requestBody) throws Exception {
    String webhookUrl = requestBody.get("webhookUrl");
    if (webhookUrl == null || webhookUrl.isBlank()) {
      throw new IllegalArgumentException("webhookUrl la bat buoc");
    }

    ConfirmWebhookResponse result = payOS.webhooks().confirm(webhookUrl);
    return ApiResponse.success("ok", result);
  }

  @GetMapping(path = "/{orderId}/invoices")
  public ApiResponse<InvoicesInfo> retrieveInvoices(@PathVariable("orderId") long orderId)
      throws Exception {
    InvoicesInfo invoicesInfo = payOS.paymentRequests().invoices().get(orderId);
    return ApiResponse.success("ok", invoicesInfo);
  }

  @GetMapping(path = "/{orderId}/invoices/{invoiceId}/download")
  public ResponseEntity<?> downloadInvoice(
      @PathVariable("orderId") long orderId, @PathVariable("invoiceId") String invoiceId) {
    try {
      FileDownloadResponse invoiceFile =
          payOS.paymentRequests().invoices().download(invoiceId, orderId);

      if (invoiceFile == null || invoiceFile.getData() == null) {
        return ResponseEntity.status(404).body(ApiResponse.error("invoice not found or empty"));
      }

      ByteArrayResource resource = new ByteArrayResource(invoiceFile.getData());

      HttpHeaders headers = new HttpHeaders();
      String contentType =
          invoiceFile.getContentType() == null
              ? MediaType.APPLICATION_PDF_VALUE
              : invoiceFile.getContentType();
      headers.set(HttpHeaders.CONTENT_TYPE, contentType);
      headers.set(
          HttpHeaders.CONTENT_DISPOSITION,
          "attachment; filename=\"" + invoiceFile.getFilename() + "\"");
      if (invoiceFile.getSize() != null) {
        headers.setContentLength(invoiceFile.getSize());
      }

      return ResponseEntity.ok().headers(headers).body(resource);
    } catch (APIException e) {
      throw new IllegalStateException(e.getErrorDesc().orElse(e.getMessage()), e);
    }
  }
}
