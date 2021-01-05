package io.harness.delegate.task.azure.appservice.webapp.response;

import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureWebAppListWebAppDeploymentSlotsResponse implements AzureAppServiceTaskResponse {
  private List<DeploymentSlotData> deploymentSlots;
}
