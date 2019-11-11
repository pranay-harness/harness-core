package io.harness.ccm.budget.entities;

import lombok.Data;

@Data
public class AlertThreshold {
  float percentage;
  AlertThresholdBase basedOn;
}
