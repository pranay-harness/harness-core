/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.azurekeyvaultconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class AzureKeyVaultConstants {
  public static final String AZURE_DEFAULT_ENCRYPTION_URL = "https://%s.vault.azure.net";
  public static final String AZURE_US_GOVERNMENT_ENCRYPTION_URL = "https://%s.vault.usgovcloudapi.net";
}
