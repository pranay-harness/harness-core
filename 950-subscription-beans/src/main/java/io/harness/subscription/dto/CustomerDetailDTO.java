package io.harness.subscription.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerDetailDTO {
  private String customerId;
  private String billingEmail;
  private String companyName;
  private String defaultSource;
}
