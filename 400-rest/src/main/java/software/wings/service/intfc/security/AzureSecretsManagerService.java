package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.AzureVaultConfig;

import java.io.IOException;
import java.util.List;

@OwnedBy(PL)
public interface AzureSecretsManagerService {
  String saveAzureSecretsManagerConfig(String accountId, AzureVaultConfig secretsManagerConfig);

  List<String> listAzureVaults(String accountId, AzureVaultConfig secretsManagerConfig) throws IOException;

  void decryptAzureConfigSecrets(AzureVaultConfig secretManagerConfig, boolean maskSecret);

  AzureVaultConfig getEncryptionConfig(String accountId, String id);

  boolean deleteConfig(String accountId, String configId);
}
