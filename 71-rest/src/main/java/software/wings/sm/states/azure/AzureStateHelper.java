package software.wings.sm.states.azure;

import io.harness.delegate.beans.azure.AzureConfigDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;

import software.wings.beans.AzureConfig;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class AzureStateHelper {
  public AzureConfigDTO createAzureConfigDTO(AzureConfig azureConfig) {
    return AzureConfigDTO.builder()
        .clientId(azureConfig.getClientId())
        .key(new SecretRefData(azureConfig.getEncryptedKey(), Scope.ACCOUNT, null))
        .tenantId(azureConfig.getTenantId())
        .build();
  }
}
