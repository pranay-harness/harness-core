package io.harness.payment.services;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerRecord {
  private String id;
  private String accountId;
  private String customerName;
  private String email;
}
