package software.wings.service.impl.security.customsecretsmanager;

import com.google.inject.Inject;

import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.service.intfc.security.CustomSecretsManagerEncryptionService;
import software.wings.service.intfc.security.CustomSecretsManagerService;

public class CustomSecretsManagerEncryptionServiceImpl implements CustomSecretsManagerEncryptionService {
  private CustomSecretsManagerService customSecretsManagerService;

  @Inject
  public CustomSecretsManagerEncryptionServiceImpl(CustomSecretsManagerService customSecretsManagerService) {
    this.customSecretsManagerService = customSecretsManagerService;
  }

  public void validateSecret(EncryptedData encryptedData, CustomSecretsManagerConfig customSecretsManagerConfig) {
    CustomSecretsManagerValidationUtils.validateVariables(
        customSecretsManagerConfig, encryptedData.getSecretVariables());
  }

  public String fetchSecret(EncryptedData encryptedData, CustomSecretsManagerConfig customSecretsManagerConfig) {
    return null;
  }
}
