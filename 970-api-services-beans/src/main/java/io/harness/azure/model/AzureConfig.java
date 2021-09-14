/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.azure.model;

import io.harness.azure.AzureEnvironmentType;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString(exclude = "key")
public class AzureConfig {
  String tenantId;
  String clientId;
  char[] key;
  private AzureEnvironmentType azureEnvironmentType;
}
