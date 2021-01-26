package io.harness.delegate.task.azure.arm.response;

import io.harness.delegate.task.azure.arm.AzureARMTaskResponse;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureARMRollbackResponse extends AzureARMTaskResponse {
  @Builder
  public AzureARMRollbackResponse(String errorMsg) {
    super(errorMsg);
  }
}
