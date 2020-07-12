package software.wings.service.impl.security;

import static io.harness.eraro.ErrorCode.RESOURCE_NOT_FOUND;
import static io.harness.exception.WingsException.USER;
import static io.harness.security.encryption.EncryptionType.LOCAL;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.service.intfc.security.SecretManager.ACCOUNT_ID_KEY;
import static software.wings.service.intfc.security.SecretManager.CREATED_AT_KEY;
import static software.wings.service.intfc.security.SecretManager.ENCRYPTION_TYPE_KEY;
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
import software.wings.beans.AzureVaultConfig;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.VaultConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.security.AwsSecretsManagerService;
import software.wings.service.intfc.security.AzureSecretsManagerService;
import software.wings.service.intfc.security.CustomSecretsManagerService;
import software.wings.service.intfc.security.CyberArkService;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.LocalEncryptionService;
import software.wings.service.intfc.security.SecretManagerConfigService;
import software.wings.service.intfc.security.VaultService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

/**
 * @author marklu on 2019-05-31
 */
@ValidateOnExecution
@Singleton
@Slf4j
public class SecretManagerConfigServiceImpl implements SecretManagerConfigService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private KmsService kmsService;
  @Inject private GcpSecretsManagerService gcpSecretsManagerService;
  @Inject private VaultService vaultService;
  @Inject private AwsSecretsManagerService secretsManagerService;
  @Inject private LocalEncryptionService localEncryptionService;
  @Inject private AzureSecretsManagerService azureSecretsManagerService;
  @Inject private CyberArkService cyberArkService;
  @Inject private CustomSecretsManagerService customSecretsManagerService;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public String save(SecretManagerConfig secretManagerConfig) {
    String accountId = secretManagerConfig.getAccountId();
    logger.info("Saving secret manager {} of type {} from account {}", secretManagerConfig.getUuid(),
        secretManagerConfig.getEncryptionType(), accountId);

    // Need to unset other secret managers if the current one to be saved is default.
    if (secretManagerConfig.isDefault()) {
      Query<SecretManagerConfig> updateQuery =
          wingsPersistence.createQuery(SecretManagerConfig.class).filter(ACCOUNT_ID_KEY, accountId);
      UpdateOperations<SecretManagerConfig> updateOperations =
          wingsPersistence.createUpdateOperations(SecretManagerConfig.class).set(IS_DEFAULT_KEY, false);
      wingsPersistence.update(updateQuery, updateOperations);
      logger.info("Set all other secret managers as non-default in account {}", accountId);
    }

    //[PL-11328] DO NOT remove this innocent redundant looking line which is actually setting the encryptionType.
    secretManagerConfig.setEncryptionType(secretManagerConfig.getEncryptionType());
    return wingsPersistence.save(secretManagerConfig);
  }

  @Override
  public String getSecretManagerName(String kmsId, String accountId) {
    SecretManagerConfig secretManagerConfig = getSecretManagerInternal(accountId, kmsId);
    if (secretManagerConfig != null) {
      return secretManagerConfig.getName();
    } else {
      logger.warn("Secret manager with id {} for account {} can't be resolved.", kmsId, accountId);
      return null;
    }
  }

  @Override
  public SecretManagerConfig getDefaultSecretManager(String accountId) {
    SecretManagerConfig secretManagerConfig = wingsPersistence.createQuery(SecretManagerConfig.class)
                                                  .field(ACCOUNT_ID_KEY)
                                                  .equal(accountId)
                                                  .filter(IS_DEFAULT_KEY, true)
                                                  .get();

    if (secretManagerConfig != null) {
      decryptEncryptionConfigSecrets(accountId, secretManagerConfig, false);
      secretManagerConfig.setDefault(true);
      return secretManagerConfig;
    }
    return getGlobalSecretManager(accountId);
  }

  @Override
  public SecretManagerConfig getGlobalSecretManager(String accountId) {
    SecretManagerConfig secretManagerConfig = null;
    if (featureFlagService.isEnabled(FeatureName.SWITCH_GLOBAL_TO_GCP_KMS, accountId)) {
      secretManagerConfig = wingsPersistence.createQuery(SecretManagerConfig.class)
                                .field(ACCOUNT_ID_KEY)
                                .equal(GLOBAL_ACCOUNT_ID)
                                .field(ENCRYPTION_TYPE_KEY)
                                .equal(EncryptionType.GCP_KMS)
                                .get();
    }

    if (secretManagerConfig == null) {
      secretManagerConfig = wingsPersistence.createQuery(SecretManagerConfig.class)
                                .field(ACCOUNT_ID_KEY)
                                .equal(GLOBAL_ACCOUNT_ID)
                                .field(ENCRYPTION_TYPE_KEY)
                                .equal(EncryptionType.KMS)
                                .get();
    }

    if (secretManagerConfig != null) {
      decryptEncryptionConfigSecrets(accountId, secretManagerConfig, false);
      secretManagerConfig.setDefault(true);
    }
    return secretManagerConfig;
  }

  @Override
  public SecretManagerConfig getSecretManager(String accountId, String entityId) {
    return getSecretManager(accountId, entityId, false);
  }

  @Override
  public SecretManagerConfig getSecretManager(String accountId, String entityId, boolean maskSecrets) {
    SecretManagerConfig secretManagerConfig = getSecretManagerInternal(accountId, entityId);
    if (secretManagerConfig != null) {
      decryptEncryptionConfigSecrets(accountId, secretManagerConfig, maskSecrets);
      secretManagerConfig.setNumOfEncryptedValue(getEncryptedDataCount(accountId, entityId));
    }
    return secretManagerConfig;
  }

  private SecretManagerConfig getSecretManagerInternal(String accountId, String entityId) {
    if (entityId.equals(accountId)) {
      return localEncryptionService.getEncryptionConfig(accountId);
    } else {
      return wingsPersistence.createQuery(SecretManagerConfig.class)
          .field(ACCOUNT_ID_KEY)
          .in(Arrays.asList(accountId, GLOBAL_ACCOUNT_ID))
          .field(ID_KEY)
          .equal(entityId)
          .get();
    }
  }

  /**
   * required for admin portal
   * @param accountIds
   * @param includeGlobalSecretManager
   * @return
   */
  @Override
  public List<Integer> getCountOfSecretManagersForAccounts(
      List<String> accountIds, boolean includeGlobalSecretManager) {
    SecretManagerConfig globalSecretManagerConfig =
        wingsPersistence.createQuery(SecretManagerConfig.class).filter(ACCOUNT_ID_KEY, GLOBAL_ACCOUNT_ID).get();

    List<SecretManagerConfig> secretManagerConfigList =
        wingsPersistence.createQuery(SecretManagerConfig.class).field(ACCOUNT_ID_KEY).in(accountIds).asList();

    Map<String, Integer> countOfSecretManagersPerAccount = accountIds.stream().collect(
        Collectors.toMap(accountId -> accountId, accountId -> globalSecretManagerConfig != null ? 1 : 0));
    secretManagerConfigList.forEach(secretManagerConfig -> {
      int secretManagerCount = countOfSecretManagersPerAccount.get(secretManagerConfig.getAccountId());
      countOfSecretManagersPerAccount.put(secretManagerConfig.getAccountId(), secretManagerCount + 1);
    });

    return accountIds.stream().map(countOfSecretManagersPerAccount::get).collect(Collectors.toList());
  }

  // This method will decrypt the secret manager's encrypted fields
  private void decryptEncryptionConfigSecrets(
      String accountId, SecretManagerConfig secretManagerConfig, boolean maskSecrets) {
    EncryptionType encryptionType = secretManagerConfig.getEncryptionType();
    switch (encryptionType) {
      case KMS:
        kmsService.decryptKmsConfigSecrets(accountId, (KmsConfig) secretManagerConfig, maskSecrets);
        break;
      case GCP_KMS:
        gcpSecretsManagerService.decryptGcpConfigSecrets((GcpKmsConfig) secretManagerConfig, maskSecrets);
        break;
      case VAULT:
        vaultService.decryptVaultConfigSecrets(accountId, (VaultConfig) secretManagerConfig, maskSecrets);
        break;
      case AWS_SECRETS_MANAGER:
        secretsManagerService.decryptAsmConfigSecrets(
            accountId, (AwsSecretsManagerConfig) secretManagerConfig, maskSecrets);
        break;
      case AZURE_VAULT:
        azureSecretsManagerService.decryptAzureConfigSecrets((AzureVaultConfig) secretManagerConfig, maskSecrets);
        break;
      case CYBERARK:
        cyberArkService.decryptCyberArkConfigSecrets(accountId, (CyberArkConfig) secretManagerConfig, maskSecrets);
        break;
      case CUSTOM:
        customSecretsManagerService.setAdditionalDetails((CustomSecretsManagerConfig) secretManagerConfig);
        break;
      case LOCAL:
        break;
      default:
        throw new IllegalArgumentException("Encryption type " + encryptionType + " is not valid");
    }
  }

  @Override
  public EncryptionType getEncryptionBySecretManagerId(String kmsId, String accountId) {
    SecretManagerConfig secretManager = getSecretManagerInternal(accountId, kmsId);
    if (secretManager == null) {
      String errorMessage =
          String.format("Secret manager with id %s for account %s can't be resolved.", kmsId, accountId);
      throw new SecretManagementException(RESOURCE_NOT_FOUND, errorMessage, USER);
    }
    return secretManager.getEncryptionType();
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
  public List<SecretManagerConfig> listSecretManagers(String accountId, boolean maskSecret) {
    return listSecretManagers(accountId, maskSecret, true);
  }

  @Override
  public List<SecretManagerConfig> listSecretManagers(
      String accountId, boolean maskSecret, boolean includeGlobalSecretManager) {
    // encryptionType null means all secret manager types.
    return listSecretManagersInternal(accountId, null, maskSecret, includeGlobalSecretManager);
  }

  @Override
  public List<SecretManagerConfig> listSecretManagersByType(
      String accountId, EncryptionType encryptionType, boolean maskSecret) {
    return listSecretManagersInternal(accountId, encryptionType, maskSecret, true);
  }

  @Override
  public List<SecretManagerConfig> getAllGlobalSecretManagers() {
    return wingsPersistence.createQuery(SecretManagerConfig.class)
        .field(ACCOUNT_ID_KEY)
        .equal(GLOBAL_ACCOUNT_ID)
        .asList();
  }

  private List<SecretManagerConfig> listSecretManagersInternal(
      String accountId, EncryptionType encryptionType, boolean maskSecret, boolean includeGlobalSecretManager) {
    List<SecretManagerConfig> rv = new ArrayList<>();

    if (isLocalEncryptionEnabled(accountId)) {
      // If account level local encryption is enabled. Mask all other encryption configs.
      SecretManagerConfig localSecretManagerConfig = localEncryptionService.getEncryptionConfig(accountId);
      localSecretManagerConfig.setNumOfEncryptedValue(
          getEncryptedDataCount(accountId, localSecretManagerConfig.getUuid()));
      rv.add(localSecretManagerConfig);
      return rv;
    } else {
      List<SecretManagerConfig> secretManagerConfigList = wingsPersistence.createQuery(SecretManagerConfig.class)
                                                              .field(ACCOUNT_ID_KEY)
                                                              .in(Arrays.asList(accountId, GLOBAL_ACCOUNT_ID))
                                                              .order(Sort.descending(CREATED_AT_KEY))
                                                              .asList();

      boolean defaultSet = false;
      List<SecretManagerConfig> globalSecretManagerConfigList = new ArrayList<>();
      for (SecretManagerConfig secretManagerConfig : secretManagerConfigList) {
        if (secretManagerConfig.getAccountId().equals(GLOBAL_ACCOUNT_ID) && !includeGlobalSecretManager) {
          continue;
        }

        if (encryptionType == null || secretManagerConfig.getEncryptionType() == encryptionType) {
          if (secretManagerConfig.getAccountId().equals(GLOBAL_ACCOUNT_ID)) {
            globalSecretManagerConfigList.add(secretManagerConfig);
          } else {
            defaultSet = secretManagerConfig.isDefault() || defaultSet;
            rv.add(secretManagerConfig);
          }
          secretManagerConfig.setNumOfEncryptedValue(getEncryptedDataCount(accountId, secretManagerConfig.getUuid()));
          decryptEncryptionConfigSecrets(accountId, secretManagerConfig, maskSecret);
        }
      }
      SecretManagerConfig globalSecretManager = getDefaultGlobalSecretManager(globalSecretManagerConfigList, accountId);
      if (globalSecretManager != null) {
        globalSecretManager.setDefault(!defaultSet);
        rv.add(globalSecretManager);
      } else if (encryptionType == null) {
        SecretManagerConfig localSecretManagerConfig = localEncryptionService.getEncryptionConfig(accountId);
        localSecretManagerConfig.setNumOfEncryptedValue(
            getEncryptedDataCount(accountId, localSecretManagerConfig.getUuid()));
        rv.add(localSecretManagerConfig);
      }
    }
    return rv;
  }

  private int getEncryptedDataCount(String accountId, String kmsId) {
    Query<EncryptedData> encryptedDataQuery = wingsPersistence.createQuery(EncryptedData.class)
                                                  .filter(EncryptedDataKeys.accountId, accountId)
                                                  .filter(EncryptedDataKeys.kmsId, kmsId);
    return (int) encryptedDataQuery.count();
  }

  private SecretManagerConfig getDefaultGlobalSecretManager(
      List<SecretManagerConfig> globalSecretManagerConfigList, String accountId) {
    if (globalSecretManagerConfigList.isEmpty()) {
      return null;
    }

    int total = calculateTotalCount(globalSecretManagerConfigList);

    if (featureFlagService.isEnabled(FeatureName.SWITCH_GLOBAL_TO_GCP_KMS, accountId)) {
      Optional<SecretManagerConfig> optionalSecretManagerConfig =
          globalSecretManagerConfigList.stream()
              .filter(secretManagerConfig -> secretManagerConfig.getEncryptionType() == EncryptionType.GCP_KMS)
              .findFirst();
      if (optionalSecretManagerConfig.isPresent()) {
        SecretManagerConfig secretManagerConfig = optionalSecretManagerConfig.get();
        secretManagerConfig.setNumOfEncryptedValue(total);
        return secretManagerConfig;
      }
    }

    Optional<SecretManagerConfig> optionalSecretManagerConfig =
        globalSecretManagerConfigList.stream()
            .filter(secretManagerConfig -> secretManagerConfig.getEncryptionType() == EncryptionType.KMS)
            .findFirst();
    if (optionalSecretManagerConfig.isPresent()) {
      SecretManagerConfig secretManagerConfig = optionalSecretManagerConfig.get();
      secretManagerConfig.setNumOfEncryptedValue(total);
      return secretManagerConfig;
    }

    return null;
  }

  private int calculateTotalCount(List<SecretManagerConfig> globalSecretManagerConfigList) {
    return globalSecretManagerConfigList.stream().mapToInt(SecretManagerConfig::getNumOfEncryptedValue).sum();
  }

  private boolean isLocalEncryptionEnabled(String accountId) {
    Account account = wingsPersistence.get(Account.class, accountId);
    return account != null && account.isLocalEncryptionEnabled();
  }
}
