package io.harness.ccm.budget;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Whether the alert is based on Actual cost or next 30 days Forecasted Cost")
public enum AlertThresholdBase {
  ACTUAL_COST,
  FORECASTED_COST
}
