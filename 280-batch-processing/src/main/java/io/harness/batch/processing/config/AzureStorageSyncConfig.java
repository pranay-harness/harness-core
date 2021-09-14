/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.config;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
public class AzureStorageSyncConfig {
  private String azureStorageAccountName;
  private String azureStorageContainerName;
  private String azureAppClientId;
  private String azureAppClientSecret;
  private String azureTenantId;
  private String azureSasToken;
  private boolean syncJobDisabled;
}
