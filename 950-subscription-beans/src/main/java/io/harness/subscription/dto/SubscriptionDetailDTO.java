package io.harness.subscription.dto;

import io.harness.ModuleType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionDetailDTO {
  private String subscriptionId;
  private String accountIdentifier;
  private String customerId;
  private ModuleType moduletype;
  private String status;
  private Long cancelAt;
  private Long canceledAt;
  private String clientSecret;
  private PendingUpdateDetailDTO pendingUpdate;
  private String latestInvoice;
  private InvoiceDetailDTO latestInvoiceDetail;
}
