package software.wings.service.intfc.security;

import software.wings.beans.AzureVaultConfig;

import java.io.IOException;
import java.util.List;

public interface AzureSecretsManagerService {
  String saveAzureSecretsManagerConfig(String accountId, AzureVaultConfig secretsManagerConfig);

  List<String> listAzureVaults(String accountId, AzureVaultConfig secretsManagerConfig) throws IOException;

  void decryptAzureConfigSecrets(AzureVaultConfig secretManagerConfig, boolean maskSecret);

  AzureVaultConfig getEncryptionConfig(String accountId, String id);
}
