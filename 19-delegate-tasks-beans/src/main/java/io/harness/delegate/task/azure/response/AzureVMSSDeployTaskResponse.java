package io.harness.delegate.task.azure.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AzureVMSSDeployTaskResponse implements AzureVMSSTaskResponse {
  private List<AzureVMInstanceData> vmInstancesAdded;
  private List<AzureVMInstanceData> vmInstancesExisting;
}
