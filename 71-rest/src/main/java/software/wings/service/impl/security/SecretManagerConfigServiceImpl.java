package software.wings.service.impl.security;

import static io.harness.security.encryption.EncryptionType.LOCAL;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.service.intfc.security.SecretManager.ACCOUNT_ID_KEY;
import static software.wings.service.intfc.security.SecretManager.CREATED_AT_KEY;
import static software.wings.service.intfc.security.SecretManager.ID_KEY;
import static software.wings.service.intfc.security.SecretManager.IS_DEFAULT_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.security.encryption.EncryptionType;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Account;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.VaultConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.service.intfc.security.AwsSecretsManagerService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.LocalEncryptionService;
import software.wings.service.intfc.security.SecretManagerConfigService;
import software.wings.service.intfc.security.VaultService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author marklu on 2019-05-31
 */
@Singleton
@Slf4j
public class SecretManagerConfigServiceImpl implements SecretManagerConfigService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private KmsService kmsService;
  @Inject private VaultService vaultService;
  @Inject private AwsSecretsManagerService secretsManagerService;
  @Inject private LocalEncryptionService localEncryptionService;

  @Override
  public String save(SecretManagerConfig secretManagerConfig) {
    logger.info("Saving secret manager {} of type {} from account {}", secretManagerConfig.getUuid(),
        secretManagerConfig.getEncryptionType(), secretManagerConfig.getAccountId());

    // Need to unset other secret managers if the current one to be saved is default.
    if (secretManagerConfig.isDefault()) {
      Query<SecretManagerConfig> updateQuery = wingsPersistence.createQuery(SecretManagerConfig.class);
      UpdateOperations<SecretManagerConfig> updateOperations =
          wingsPersistence.createUpdateOperations(SecretManagerConfig.class).set(IS_DEFAULT_KEY, false);
      wingsPersistence.update(updateQuery, updateOperations);
      logger.info("Set all other secret managers as non-default");
    }

    secretManagerConfig.setEncryptionType(secretManagerConfig.getEncryptionType());

    return wingsPersistence.save(secretManagerConfig);
  }

  @Override
  public SecretManagerConfig getDefaultSecretManager(String accountId) {
    SecretManagerConfig secretManagerConfig = wingsPersistence.createQuery(SecretManagerConfig.class)
                                                  .field(ACCOUNT_ID_KEY)
                                                  .equal(accountId)
                                                  .filter(IS_DEFAULT_KEY, true)
                                                  .get();
    if (secretManagerConfig == null) {
      secretManagerConfig =
          wingsPersistence.createQuery(SecretManagerConfig.class).field(ACCOUNT_ID_KEY).equal(GLOBAL_ACCOUNT_ID).get();
    }

    SecretManagerConfig encryptionConfig =
        secretManagerConfig == null ? null : getEncryptionConfigInternal(accountId, secretManagerConfig);
    if (encryptionConfig != null) {
      encryptionConfig.setDefault(true);
    }
    return encryptionConfig;
  }

  @Override
  public SecretManagerConfig getSecretManager(String accountId, String entityId) {
    SecretManagerConfig secretManagerConfig = wingsPersistence.createQuery(SecretManagerConfig.class)
                                                  .field(ACCOUNT_ID_KEY)
                                                  .equal(accountId)
                                                  .field(ID_KEY)
                                                  .equal(entityId)
                                                  .get();
    return getEncryptionConfigInternal(accountId, secretManagerConfig);
  }

  private SecretManagerConfig getEncryptionConfigInternal(String accountId, SecretManagerConfig secretManagerConfig) {
    SecretManagerConfig encryptionConfig;
    EncryptionType encryptionType = secretManagerConfig.getEncryptionType();
    String encryptionConfigId = secretManagerConfig.getUuid();
    switch (encryptionType) {
      case KMS:
        encryptionConfig = kmsService.getKmsConfig(accountId, encryptionConfigId);
        break;
      case VAULT:
        encryptionConfig = vaultService.getVaultConfig(accountId, encryptionConfigId);
        break;
      case AWS_SECRETS_MANAGER:
        encryptionConfig = secretsManagerService.getAwsSecretsManagerConfig(accountId, encryptionConfigId);
        break;
      case LOCAL:
        encryptionConfig = localEncryptionService.getEncryptionConfig(accountId);
        break;
      default:
        throw new IllegalArgumentException("Encryption type " + encryptionType + " is not valid");
    }

    return encryptionConfig;
  }

  @Override
  public EncryptionType getEncryptionType(String accountId) {
    final EncryptionType encryptionType;
    if (isLocalEncryptionEnabled(accountId)) {
      // HAR-8025: Respect the account level 'localEncryptionEnabled' configuration.
      encryptionType = LOCAL;
    } else {
      SecretManagerConfig defaultSecretManagerConfig = getDefaultSecretManager(accountId);
      if (defaultSecretManagerConfig == null) {
        encryptionType = LOCAL;
      } else {
        encryptionType = defaultSecretManagerConfig.getEncryptionType();
      }
    }

    return encryptionType;
  }

  @Override
  public List<SecretManagerConfig> listSecretManagers(String accountId) {
    // encryptionType null means all secret manager types.
    return listSecretManagersInternal(accountId, null, true);
  }

  @Override
  public List<SecretManagerConfig> listSecretManagersByType(
      String accountId, EncryptionType encryptionType, boolean maskSecret) {
    return listSecretManagersInternal(accountId, encryptionType, maskSecret);
  }

  private List<SecretManagerConfig> listSecretManagersInternal(
      String accountId, EncryptionType encryptionType, boolean maskSecret) {
    List<SecretManagerConfig> rv = new ArrayList<>();

    if (isLocalEncryptionEnabled(accountId)) {
      // If account level local encryption is enabled. Mask all other encryption configs.
      return rv;
    } else {
      List<SecretManagerConfig> secretManagerConfigList = wingsPersistence.createQuery(SecretManagerConfig.class)
                                                              .field(ACCOUNT_ID_KEY)
                                                              .in(Arrays.asList(accountId, GLOBAL_ACCOUNT_ID))
                                                              .order(Sort.descending(CREATED_AT_KEY))
                                                              .asList();

      boolean defaultSet = false;
      SecretManagerConfig globalSecretManager = null;
      for (SecretManagerConfig secretManagerConfig : secretManagerConfigList) {
        if (encryptionType == null || secretManagerConfig.getEncryptionType() == encryptionType) {
          rv.add(secretManagerConfig);

          defaultSet = secretManagerConfig.isDefault() || defaultSet;
          globalSecretManager =
              secretManagerConfig.getAccountId().equals(GLOBAL_ACCOUNT_ID) ? secretManagerConfig : globalSecretManager;
          Query<EncryptedData> encryptedDataQuery = wingsPersistence.createQuery(EncryptedData.class)
                                                        .filter(EncryptedDataKeys.accountId, accountId)
                                                        .filter(EncryptedDataKeys.kmsId, secretManagerConfig.getUuid());
          secretManagerConfig.setNumOfEncryptedValue(encryptedDataQuery.asKeyList().size());

          switch (secretManagerConfig.getEncryptionType()) {
            case AWS_SECRETS_MANAGER:
              secretsManagerService.decryptAsmConfigSecrets(
                  accountId, (AwsSecretsManagerConfig) secretManagerConfig, maskSecret);
              break;
            case KMS:
              kmsService.decryptKmsConfigSecrets(accountId, (KmsConfig) secretManagerConfig, maskSecret);
              break;
            case VAULT:
              vaultService.decryptVaultConfigSecrets(accountId, (VaultConfig) secretManagerConfig, maskSecret);
              break;
            default:
              // Do nothing;
              break;
          }
        }
      }

      if (!defaultSet && globalSecretManager != null) {
        globalSecretManager.setDefault(true);
      }
    }

    return rv;
  }

  private boolean isLocalEncryptionEnabled(String accountId) {
    Account account = wingsPersistence.get(Account.class, accountId);
    return account != null && account.isLocalEncryptionEnabled();
  }
}
