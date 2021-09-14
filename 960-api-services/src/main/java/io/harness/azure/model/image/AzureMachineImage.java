/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.azure.model.image;

import io.harness.azure.model.AzureMachineImageArtifact;

import com.microsoft.azure.management.compute.implementation.VirtualMachineScaleSetInner;

public abstract class AzureMachineImage {
  protected AzureMachineImageArtifact image;

  public AzureMachineImage(AzureMachineImageArtifact image) {
    this.image = image;
  }

  public void updateVMSSInner(VirtualMachineScaleSetInner inner) {
    updateVirtualMachineScaleSetOSProfile(inner);
    updateVirtualMachineScaleSetStorageProfile(inner);
  }

  protected abstract void updateVirtualMachineScaleSetOSProfile(VirtualMachineScaleSetInner inner);

  protected abstract void updateVirtualMachineScaleSetStorageProfile(VirtualMachineScaleSetInner inner);
}
