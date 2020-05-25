package software.wings.service.intfc.security;

import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;

public interface CustomSecretsManagerEncryptionService {
  void validateSecret(EncryptedData encryptedData, CustomSecretsManagerConfig customSecretsManagerConfig);
  String fetchSecret(EncryptedData encryptedData, CustomSecretsManagerConfig customSecretsManagerConfig);
}
