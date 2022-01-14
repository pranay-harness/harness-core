package io.harness.delegate.task.azure.response;

import io.harness.azure.model.VirtualMachineScaleSetData;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureVMSSListVirtualMachineScaleSetsResponse implements AzureVMSSTaskResponse {
  private List<VirtualMachineScaleSetData> virtualMachineScaleSets;
}
