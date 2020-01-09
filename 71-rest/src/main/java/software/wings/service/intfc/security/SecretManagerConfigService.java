package software.wings.service.intfc.security;

import io.harness.security.encryption.EncryptionType;
import software.wings.beans.SecretManagerConfig;

import java.util.List;

/**
 * Created by mark.lu on 5/31/2019.
 */
public interface SecretManagerConfigService {
  String save(SecretManagerConfig secretManagerConfig);

  EncryptionType getEncryptionType(String accountId);

  EncryptionType getEncryptionBySecretManagerId(String kmsId, String accountId);

  List<SecretManagerConfig> listSecretManagers(String accountId, boolean maskSecret);

  List<SecretManagerConfig> listSecretManagers(
      String accountId, boolean maskSecret, boolean includeGlobalSecretManager);

  List<SecretManagerConfig> listSecretManagersByType(
      String accountId, EncryptionType encryptionType, boolean maskSecret);

  SecretManagerConfig getDefaultSecretManager(String accountId);

  SecretManagerConfig getGlobalSecretManager(String accountId);

  List<SecretManagerConfig> getAllGlobalSecretManagers();

  SecretManagerConfig getSecretManager(String accountId, String entityId);
}
