package software.wings.service.intfc.security;

import io.harness.security.encryption.EncryptedRecord;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;

public interface CustomSecretsManagerDelegateService {
  boolean isExecutableOnDelegate(CustomSecretsManagerConfig customSecretsManagerConfig);
  char[] fetchSecret(EncryptedRecord encryptedData, CustomSecretsManagerConfig customSecretsManagerConfig);
}
