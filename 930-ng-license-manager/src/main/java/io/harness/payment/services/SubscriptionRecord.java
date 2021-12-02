package io.harness.payment.services;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionRecord {
  private String customerId;
  private List<SubscriptionItem> items;
}
