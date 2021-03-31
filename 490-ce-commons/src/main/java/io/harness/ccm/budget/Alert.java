package io.harness.ccm.budget;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Alert {
  double percentage;
  AlertThresholdBase basedOn;
  double budgetedAmount;
  long crossedAt;
}
