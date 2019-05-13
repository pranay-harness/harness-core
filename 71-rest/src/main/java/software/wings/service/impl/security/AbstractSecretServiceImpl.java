package software.wings.service.impl.security;

import static software.wings.service.intfc.security.SecretManager.ACCOUNT_ID_KEY;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptionType;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.security.KmsService;

import java.util.List;
import java.util.UUID;

/**
 * Created by rsingh on 11/6/17.
 */
@Slf4j
public abstract class AbstractSecretServiceImpl {
  static final String SECRET_MASK = "**************";
  static final String REASON_KEY = "reason";
  static final String IS_DEFAULT_KEY = "isDefault";
  static final String ID_KEY = "_id";
  static final String NAME_KEY = "name";

  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected DelegateProxyFactory delegateProxyFactory;
  @Inject private KmsService kmsService;

  EncryptedData encryptLocal(char[] value) {
    final String encryptionKey = UUID.randomUUID().toString();
    final SimpleEncryption simpleEncryption = new SimpleEncryption(encryptionKey);
    char[] encryptChars = simpleEncryption.encryptChars(value);

    return EncryptedData.builder()
        .encryptionKey(encryptionKey)
        .encryptedValue(encryptChars)
        .encryptionType(EncryptionType.LOCAL)
        .build();
  }

  char[] decryptLocal(EncryptedData data) {
    final SimpleEncryption simpleEncryption = new SimpleEncryption(data.getEncryptionKey());
    return simpleEncryption.decryptChars(data.getEncryptedValue());
  }

  char[] decryptKey(char[] key) {
    final EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, new String(key));
    return decryptLocal(encryptedData);
  }

  char[] decryptVaultToken(EncryptedData encryptedVaultToken) {
    EncryptionType encryptionType = encryptedVaultToken.getEncryptionType();
    String accountId = encryptedVaultToken.getAccountId();
    switch (encryptionType) {
      case LOCAL:
        // Newly created Vault config should never be encrypted by KMS.
        return decryptLocal(encryptedVaultToken);
      case KMS:
        // This is for backward compatibility. Existing vault configs may still have their root token
        // encrypted by KMS.
        KmsConfig kmsConfig = kmsService.getKmsConfig(accountId, encryptedVaultToken.getKmsId());
        char[] decrypted = kmsService.decrypt(encryptedVaultToken, accountId, kmsConfig);

        // Runtime migration of vault root token from KMS to LOCAL at time of reading
        try {
          EncryptedData reencryptedData = encryptLocal(decrypted);
          encryptedVaultToken.setEncryptionType(reencryptedData.getEncryptionType());
          encryptedVaultToken.setEncryptionKey(reencryptedData.getEncryptionKey());
          encryptedVaultToken.setEncryptedValue(reencryptedData.getEncryptedValue());
          encryptedVaultToken.setKmsId(null);
          wingsPersistence.save(encryptedVaultToken);
          logger.info(
              "Successfully migrated vault token {} from KMS to LOCAL encryption.", encryptedVaultToken.getName());
        } catch (WingsException e) {
          logger.warn(
              "Failed in migrating vault token " + encryptedVaultToken.getName() + " from KMS to LOCAL encryption.", e);
        }

        return decrypted;
      default:
        throw new IllegalStateException("Unexpected Vault root token encryption type: " + encryptionType);
    }
  }

  protected boolean updateCurrentEncryptionConfigsToNonDefault(String accountId) {
    boolean updated = false;
    Query<KmsConfig> kmsConfigQuery = wingsPersistence.createQuery(KmsConfig.class).filter(ACCOUNT_ID_KEY, accountId);
    List<KmsConfig> kmsConfigs = kmsConfigQuery.asList();
    for (KmsConfig kmsConfig : kmsConfigs) {
      if (kmsConfig.isDefault()) {
        kmsConfig.setDefault(false);
        wingsPersistence.save(kmsConfig);
        updated = true;
      }
    }
    Query<VaultConfig> vaultConfigQuery =
        wingsPersistence.createQuery(VaultConfig.class).filter(ACCOUNT_ID_KEY, accountId);
    List<VaultConfig> vaultConfigs = vaultConfigQuery.asList();
    for (VaultConfig vaultConfig : vaultConfigs) {
      if (vaultConfig.isDefault()) {
        vaultConfig.setDefault(false);
        wingsPersistence.save(vaultConfig);
        updated = true;
      }
    }
    Query<AwsSecretsManagerConfig> awsSecretsManagerConfigQuery =
        wingsPersistence.createQuery(AwsSecretsManagerConfig.class).filter(ACCOUNT_ID_KEY, accountId);
    List<AwsSecretsManagerConfig> awsSecretsManagerConfigs = awsSecretsManagerConfigQuery.asList();
    for (AwsSecretsManagerConfig secretsManagerConfig : awsSecretsManagerConfigs) {
      if (secretsManagerConfig.isDefault()) {
        secretsManagerConfig.setDefault(false);
        wingsPersistence.save(secretsManagerConfig);
        updated = true;
      }
    }

    return updated;
  }
}
