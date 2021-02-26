package io.harness.delegate.beans.connector.ceazure;

import io.swagger.annotations.ApiModel;
import lombok.Getter;

@ApiModel("CEAzureFeatures")
public enum CEAzureFeatures {
  BILLING("Cost Management And Billing Export"),
  OPTIMIZATION("Instance Cost Optimization");

  @Getter private final String description;
  CEAzureFeatures(String description) {
    this.description = description;
  }
}
