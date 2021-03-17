package io.harness.delegate.task.azure.arm;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureARMPreDeploymentData {
  String resourceGroup;
  String subscriptionId;
  String resourceGroupTemplateJson;
}
