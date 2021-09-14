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
public class AzureWebClientContext extends AzureClientContext {
  private String appName;

  @Builder
  AzureWebClientContext(@NonNull AzureConfig azureConfig, @NonNull String subscriptionId,
      @NonNull String resourceGroupName, @NonNull String appName) {
    super(azureConfig, subscriptionId, resourceGroupName);
    this.appName = appName;
  }
}
