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
