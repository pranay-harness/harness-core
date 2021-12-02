package io.harness.payment.services;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionItem {
  private String priceId;
  private Long quantity;
}
