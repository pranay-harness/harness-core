/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class AzureVirtualMachineScaleSet extends AzureResourceReference {
  @Builder
  private AzureVirtualMachineScaleSet(
      String name, String resourceGroup, String subscriptionId, String type, String id) {
    super(name, resourceGroup, subscriptionId, type, id);
  }
}
