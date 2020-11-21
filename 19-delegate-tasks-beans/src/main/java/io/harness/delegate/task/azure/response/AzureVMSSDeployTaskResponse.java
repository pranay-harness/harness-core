package io.harness.delegate.task.azure.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureVMSSDeployTaskResponse implements AzureVMSSTaskResponse {
  private List<AzureVMInstanceData> vmInstancesAdded;
  private List<AzureVMInstanceData> vmInstancesExisting;
}
