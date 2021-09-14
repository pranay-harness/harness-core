/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.azure.model.image;

import static io.harness.azure.model.AzureMachineImageArtifact.OSType.LINUX;
import static io.harness.azure.model.AzureMachineImageArtifact.OSType.WINDOWS;

import io.harness.azure.model.AzureMachineImageArtifact;
import io.harness.azure.model.AzureMachineImageArtifact.MachineImageReference;
import io.harness.azure.model.AzureMachineImageArtifact.MachineImageReference.OsState;
import io.harness.azure.model.AzureMachineImageArtifact.OSType;

import com.microsoft.azure.management.compute.ImageReference;
import com.microsoft.azure.management.compute.LinuxConfiguration;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetOSProfile;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetStorageProfile;
import com.microsoft.azure.management.compute.WindowsConfiguration;
import com.microsoft.azure.management.compute.implementation.VirtualMachineScaleSetInner;

public class SharedGalleryImage extends AzureMachineImage {
  public SharedGalleryImage(AzureMachineImageArtifact image) {
    super(image);
  }

  @Override
  protected void updateVirtualMachineScaleSetOSProfile(VirtualMachineScaleSetInner inner) {
    OsState osState = image.getImageReference().getOsState();
    if (OsState.SPECIALIZED == osState) {
      // specialized images should not have an osProfile associated with them
      inner.virtualMachineProfile().withOsProfile(null);
      return;
    }
    // only applied on generalized images
    VirtualMachineScaleSetOSProfile osProfile = inner.virtualMachineProfile().osProfile();
    OSType osType = image.getOsType();
    if (LINUX == osType) {
      osProfile.withLinuxConfiguration(new LinuxConfiguration());
      osProfile.linuxConfiguration().withDisablePasswordAuthentication(false);
      osProfile.linuxConfiguration().withProvisionVMAgent(true);
      osProfile.withWindowsConfiguration(null);
    } else if (WINDOWS == osType) {
      osProfile.withWindowsConfiguration(new WindowsConfiguration());
      osProfile.windowsConfiguration().withProvisionVMAgent(true);
      osProfile.windowsConfiguration().withEnableAutomaticUpdates(true);
      osProfile.withLinuxConfiguration(null);
    }
  }

  @Override
  protected void updateVirtualMachineScaleSetStorageProfile(VirtualMachineScaleSetInner inner) {
    VirtualMachineScaleSetStorageProfile storageProfile = inner.virtualMachineProfile().storageProfile();
    storageProfile.withImageReference(getManagedImageReference());
  }

  private ImageReference getManagedImageReference() {
    MachineImageReference artifactImageReference = image.getImageReference();
    ImageReference imageReference = new ImageReference();
    imageReference.withId(artifactImageReference.getId());
    return imageReference;
  }
}
