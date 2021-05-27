package io.harness.delegate.beans.connector.ceawsconnector;

import io.swagger.annotations.ApiModel;
import lombok.Getter;

@ApiModel("CEAwsFeatures")
public enum CEAwsFeatures {
  CUR("Cost And Usage Report Billing"),
  VISIBILITY("Receive Events For Cloud Accounts"),
  OPTIMIZATION("Lightwing Cost Optimization");

  @Getter private final String description;
  CEAwsFeatures(String description) {
    this.description = description;
  }
}
