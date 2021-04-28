package io.harness.delegate.beans.connector.gcpccm;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import lombok.Getter;

@ApiModel("GcpCloudCostFeatures")
@OwnedBy(CE)
public enum GcpCloudCostFeatures {
  BILLING("Cost Management And Billing Export");

  @Getter private final String description;
  GcpCloudCostFeatures(String description) {
    this.description = description;
  }
}
