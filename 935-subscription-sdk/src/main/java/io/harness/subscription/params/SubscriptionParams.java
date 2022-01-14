package io.harness.subscription.params;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionParams {
  private String accountIdentifier;
  private String subscriptionId;
  private String customerId;
  private String paymentIntentId;
  private List<ItemParams> items;
}
