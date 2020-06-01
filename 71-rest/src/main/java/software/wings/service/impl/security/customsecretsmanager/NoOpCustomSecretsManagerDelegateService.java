package software.wings.service.impl.security.customsecretsmanager;

import com.google.inject.Singleton;

import io.harness.security.encryption.EncryptedRecord;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.service.intfc.security.CustomSecretsManagerDelegateService;

@Singleton
public class NoOpCustomSecretsManagerDelegateService implements CustomSecretsManagerDelegateService {
  @Override
  public boolean isExecutableOnDelegate(CustomSecretsManagerConfig customSecretsManagerConfig) {
    return false;
  }

  @Override
  public char[] fetchSecret(EncryptedRecord encryptedData, CustomSecretsManagerConfig customSecretsManagerConfig) {
    return null;
  }
}
