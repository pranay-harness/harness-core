package io.harness.ccm.commons.beans.config;

import io.swagger.annotations.ApiModel;
import lombok.Getter;

@ApiModel("CEFeatures")
public enum CEFeatures {
  BILLING("Cost Management And Billing Export"),
  OPTIMIZATION("Lightwing Cost Optimization"),
  VISIBILITY("Receive Events For Cloud Accounts");

  @Getter private final String description;
  CEFeatures(String description) {
    this.description = description;
  }
}
