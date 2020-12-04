package io.harness.delegate.task.azure.appservice.webapp.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureAppDeploymentData {
  private String instanceId;
  private String resourceGroup;
  private String subscriptionId;
  private String appName;
  private String deploySlot;
  private String deploySlotId;
  private String appServicePlanId;
  private String hostName;
}
