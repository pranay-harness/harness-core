package io.harness.delegate.task.azure.arm.response;

import io.harness.delegate.task.azure.arm.AzureARMTaskResponse;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureBlueprintDeploymentResponse extends AzureARMTaskResponse {
  @Builder
  public AzureBlueprintDeploymentResponse(String outputs, String errorMsg) {
    super(errorMsg);
  }
}
