package io.harness.subscription.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoiceDetailDTO {
  private List<ItemDTO> items;
  private String subscriptionId;
  private String invoiceId;
  private Long totalAmount;
  private Long periodStart;
  private Long periodEnd;
  private Long nextPaymentAttempt;
  private Long amountDue;
  private PaymentIntentDetailDTO paymentIntent;
}
