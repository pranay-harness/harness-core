package io.harness.subscription.params;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ItemParams {
  private String priceId;
  private Long quantity;
}
