package io.harness.delegate.task.azure.response;

import io.harness.azure.model.VirtualMachineScaleSetData;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureVMSSGetVirtualMachineScaleSetResponse implements AzureVMSSTaskResponse {
  private VirtualMachineScaleSetData virtualMachineScaleSet;
}
