package io.harness.azure.context;

import io.harness.azure.model.ARMScopeType;
import io.harness.azure.model.AzureConfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ARMDeploymentSteadyStateContext {
  private String tenantId;
  private String managementGroupId;
  private String subscriptionId;
  private String resourceGroup;
  private String deploymentName;
  private AzureConfig azureConfig;
  private ARMScopeType scopeType;
  private long steadyCheckTimeoutInMinutes;
  private long statusCheckIntervalInSeconds;
}
