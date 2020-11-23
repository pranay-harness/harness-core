package io.harness.delegate.task.azure.appservice.webapp.response;

import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureWebAppSlotResizeResponse implements AzureAppServiceTaskResponse {
  private AzureAppServicePreDeploymentData preDeploymentData;
}
