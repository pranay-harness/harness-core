/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.azure.context;

import io.harness.azure.model.AzureConfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class AzureClientContext {
  @NonNull private AzureConfig azureConfig;
  @NonNull private String subscriptionId;
  @NonNull private String resourceGroupName;
}
