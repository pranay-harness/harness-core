package io.harness.delegate.task.azure.response;

import io.harness.azure.model.VirtualMachineScaleSetData;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AzureVMSSListVirtualMachineScaleSetsResponse implements AzureVMSSTaskResponse {
  private List<VirtualMachineScaleSetData> virtualMachineScaleSets;
}
