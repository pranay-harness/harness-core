package io.harness.ccm.budget.entities;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlertThreshold {
  double percentage;
  AlertThresholdBase basedOn;
  int alertsSent;
  long crossedAt;
}
