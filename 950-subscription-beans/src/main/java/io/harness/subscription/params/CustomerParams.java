package io.harness.subscription.params;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerParams {
  private String accountIdentifier;
  private String customerId;
  private String name;
  private String billingContactEmail;
}
