package software.wings.service.intfc.security;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;

public interface CustomSecretsManagerEncryptionService {
  EncryptedDataDetail buildEncryptedDataDetail(
      EncryptedData encryptedData, CustomSecretsManagerConfig customSecretsManagerConfig);
  void validateSecret(EncryptedData encryptedData, CustomSecretsManagerConfig customSecretsManagerConfig);
}
