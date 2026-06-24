package databreeze.api.payos;

import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.PayOS;

import databreeze.dto.payments.CreatePaymentLinkRequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

@RestController
@RequestMapping("/api/v1/payos/checkout")
@ConditionalOnProperty(prefix = "app.payos", name = "enabled", havingValue = "true", matchIfMissing = true)
@SecurityRequirement(name = "bearer")
public class CheckoutController {
  private final PayOS payOS;

  public CheckoutController(PayOS payOS) {
    super();
    this.payOS = payOS;
  }

  @PostMapping("/redirect")
  public ResponseEntity<Void> checkout(@Valid @RequestBody CreatePaymentLinkRequestBody requestBody)
      throws Exception {
    long orderCode =
        requestBody.getOrderCode() == null ? System.currentTimeMillis() / 1000 : requestBody.getOrderCode();
    long price = requestBody.getPrice();
    int quantity = requestBody.getQuantity() == null ? 1 : requestBody.getQuantity();

    CreatePaymentLinkRequest paymentData =
        CreatePaymentLinkRequest.builder()
            .orderCode(orderCode)
            .amount(price)
            .description(requestBody.getDescription())
            .returnUrl(requestBody.getReturnUrl())
            .cancelUrl(requestBody.getCancelUrl())
            .item(
                PaymentLinkItem.builder()
                    .name(requestBody.getProductName())
                    .price(price)
                    .quantity(quantity)
                    .build())
            .build();

    CreatePaymentLinkResponse data = payOS.paymentRequests().create(paymentData);
    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(data.getCheckoutUrl())).build();
  }
}
