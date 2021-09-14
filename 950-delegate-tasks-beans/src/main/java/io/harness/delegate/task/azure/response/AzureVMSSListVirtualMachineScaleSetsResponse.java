/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
