package io.harness.azure.model;

import io.harness.azure.AzureEnvironmentType;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString(exclude = "key")
public class AzureConfig {
  String tenantId;
  String clientId;
  char[] key;
  private AzureEnvironmentType azureEnvironmentType;
}
