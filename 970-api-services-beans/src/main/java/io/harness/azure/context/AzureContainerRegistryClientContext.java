/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.azure.context;

import io.harness.azure.model.AzureConfig;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureContainerRegistryClientContext extends AzureClientContext {
  @NonNull private String registryName;

  @Builder
  AzureContainerRegistryClientContext(@NonNull AzureConfig azureConfig, @NonNull String subscriptionId,
      @NonNull String resourceGroupName, @NonNull String registryName) {
    super(azureConfig, subscriptionId, resourceGroupName);
    this.registryName = registryName;
  }
}
