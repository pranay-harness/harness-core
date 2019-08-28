package software.wings.service.impl.security;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.AZURE_KEY_VAULT_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static software.wings.service.intfc.security.SecretManager.ACCOUNT_ID_KEY;
import static software.wings.service.intfc.security.SecretManager.SECRET_NAME_KEY;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.SecretBundle;
import com.microsoft.azure.management.keyvault.Vault;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasName;
import com.mongodb.DuplicateKeyException;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptionType;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.Query;
import software.wings.beans.AzureVaultConfig;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.service.intfc.security.AzureSecretsManagerService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class AzureSecretsManagerServiceImpl extends AbstractSecretServiceImpl implements AzureSecretsManagerService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AzureHelperService azureHelperService;
  private static final String SECRET_KEY_NAME_SUFFIX = "_secretKey";

  @Override
  public String saveAzureSecretsManagerConfig(String accountId, AzureVaultConfig azureVautConfig) {
    checkIfSecretsManagerConfigCanBeCreatedOrUpdated(accountId);
    AzureVaultConfig savedAzureVaultConfig = null;
    azureVautConfig.setAccountId(accountId);

    boolean credentialChanged = false;

    if (isNotEmpty(azureVautConfig.getUuid())) {
      savedAzureVaultConfig = getAzureVaultConfig(azureVautConfig.getUuid());
      if (SECRET_MASK.equals(azureVautConfig.getSecretKey())) {
        savedAzureVaultConfig.setSecretKey(savedAzureVaultConfig.getSecretKey());
      }
      credentialChanged = !Objects.equals(azureVautConfig.getSecretKey(), savedAzureVaultConfig.getSecretKey())
          || !Objects.equals(azureVautConfig.getClientId(), savedAzureVaultConfig.getClientId())
          || !Objects.equals(azureVautConfig.getTenantId(), savedAzureVaultConfig.getTenantId())
          || !Objects.equals(azureVautConfig.getSubscription(), savedAzureVaultConfig.getSubscription());

      // Secret fields un-decrypted azure config
      savedAzureVaultConfig = wingsPersistence.get(AzureVaultConfig.class, azureVautConfig.getUuid());
    }

    // Validate every time when secret manager config change submitted
    validateAzureVaultConfig(azureVautConfig);

    if (!credentialChanged) {
      savedAzureVaultConfig.setName(azureVautConfig.getName());
      savedAzureVaultConfig.setDefault(azureVautConfig.isDefault());
      savedAzureVaultConfig.setVaultName(azureVautConfig.getVaultName());

      return secretManagerConfigService.save(savedAzureVaultConfig);
    }

    EncryptedData secretKeyEncryptedData = getEncryptedDataForSecretField(
        azureVautConfig, azureVautConfig, azureVautConfig.getSecretKey(), SECRET_KEY_NAME_SUFFIX);
    azureVautConfig.setSecretKey(null);
    String secretsManagerConfigId;

    try {
      secretsManagerConfigId = secretManagerConfigService.save(azureVautConfig);
    } catch (DuplicateKeyException e) {
      throw new WingsException(AZURE_KEY_VAULT_OPERATION_ERROR, USER_SRE)
          .addParam(REASON_KEY, "Another Azure vault secret configuration with the same name or URL exists");
    }

    // Create a LOCAL encrypted record for Azure secret key
    String secretKeyEncryptedDataId =
        saveSecretField(azureVautConfig, secretsManagerConfigId, secretKeyEncryptedData, SECRET_KEY_NAME_SUFFIX);
    azureVautConfig.setSecretKey(secretKeyEncryptedDataId);

    return secretManagerConfigService.save(azureVautConfig);
  }

  private String saveSecretField(AzureVaultConfig secretsManagerConfig, String configId,
      EncryptedData secretFieldEncryptedData, String secretNameSuffix) {
    String secretFieldEncryptedDataId = null;
    if (secretFieldEncryptedData != null) {
      secretFieldEncryptedData.setAccountId(secretsManagerConfig.getAccountId());
      secretFieldEncryptedData.addParent(configId);
      secretFieldEncryptedData.setType(SettingVariableTypes.AZURE);
      secretFieldEncryptedData.setName(secretsManagerConfig.getName() + secretNameSuffix);
      secretFieldEncryptedDataId = wingsPersistence.save(secretFieldEncryptedData);
    }
    return secretFieldEncryptedDataId;
  }

  private EncryptedData getEncryptedDataForSecretField(AzureVaultConfig savedSecretsManagerConfig,
      AzureVaultConfig secretsManagerConfig, String secretValue, String secretNameSuffix) {
    EncryptedData encryptedData = isNotEmpty(secretValue) ? encryptLocal(secretValue.toCharArray()) : null;
    if (savedSecretsManagerConfig != null && encryptedData != null) {
      // Get by auth token encrypted record by Id or name.
      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
      query.criteria(ACCOUNT_ID_KEY)
          .equal(secretsManagerConfig.getAccountId())
          .or(query.criteria(ID_KEY).equal(secretsManagerConfig.getSecretKey()),
              query.criteria(SECRET_NAME_KEY).equal(secretsManagerConfig.getName() + secretNameSuffix));
      EncryptedData savedEncryptedData = query.get();
      if (savedEncryptedData != null) {
        savedEncryptedData.setEncryptionKey(encryptedData.getEncryptionKey());
        savedEncryptedData.setEncryptedValue(encryptedData.getEncryptedValue());
        encryptedData = savedEncryptedData;
      }
    }
    return encryptedData;
  }

  @Override
  public List<String> listAzureVaults(String accountId, AzureVaultConfig secretsManagerConfig) {
    secretsManagerConfig.setAccountId(accountId);
    if (SECRET_MASK.equals(secretsManagerConfig.getSecretKey())) {
      decryptAzureConfigSecrets(secretsManagerConfig, false);
    }
    List<Vault> vaultList = azureHelperService.listVaults(accountId, secretsManagerConfig);
    return vaultList.stream().map(HasName::name).collect(Collectors.toList());
  }

  public void decryptAzureConfigSecrets(AzureVaultConfig secretManagerConfig, boolean maskSecret) {
    if (maskSecret) {
      secretManagerConfig.maskSecrets();
    } else {
      AzureVaultConfig currentConfig = getAzureVaultConfig(secretManagerConfig.getUuid());
      Preconditions.checkNotNull(
          currentConfig, "Azure settings with id: " + secretManagerConfig.getUuid() + " not found in database.");
      EncryptedData secretData = wingsPersistence.get(EncryptedData.class, currentConfig.getSecretKey());
      Preconditions.checkNotNull(secretData, "encrypted secret key can't be null for " + secretManagerConfig);
      secretManagerConfig.setSecretKey(new String(decryptLocal(secretData)));
    }
  }

  private AzureVaultConfig getAzureVaultConfig(String id) {
    return wingsPersistence.get(AzureVaultConfig.class, id);
  }

  private KeyVaultClient getAzureVaultClient(AzureVaultConfig azureVaultConfig) {
    decryptAzureConfigSecrets(azureVaultConfig, false);
    return KeyVaultADALAuthenticator.getClient(azureVaultConfig.getClientId(), azureVaultConfig.getSecretKey());
  }

  @Override
  public AzureVaultConfig getEncryptionConfig(String accountId, String id) {
    AzureVaultConfig secretsManagerConfig = wingsPersistence.get(AzureVaultConfig.class, id);
    Preconditions.checkNotNull(
        secretsManagerConfig, String.format("Azure vault config not found for id: %s in account: %s", id, accountId));
    decryptAzureConfigSecrets(secretsManagerConfig, false);
    return secretsManagerConfig;
  }

  /**
   * If the test test fails, throw an exception, otherwise bo return.
   * @param encryptionConfig - Azure encryption config to be used for initiating a connection with Azure vault.
   */
  @Override
  public void validateAzureVaultConfig(AzureVaultConfig encryptionConfig) {
    try {
      KeyVaultClient client = getAzureVaultClient(encryptionConfig);

      // Azure vault returns null if the secret isn not found in the db. Otherwise it returns a secretBundle. If the url
      // is incorrect or it doesn't have access, an exception will be thrown
      SecretBundle someRandomString = client.getSecret(encryptionConfig.getEncryptionServiceUrl(), "someRandomString");
      logger.info("Azure vault connection validation was successful for vault: {} and accountId: {}",
          encryptionConfig.getName(), encryptionConfig.getAccountId());
    } catch (Exception ex) {
      logger.error("Failed to validate azure config with id {} for account {}", encryptionConfig.getUuid(),
          encryptionConfig.getAccountId(), ex);
      throw new WingsException(ErrorCode.AZURE_KEY_VAULT_OPERATION_ERROR);
    }
  }

  public boolean deleteConfig(String accountId, String configId) {
    final long count = wingsPersistence.createQuery(EncryptedData.class)
                           .filter(ACCOUNT_ID_KEY, accountId)
                           .filter(EncryptedDataKeys.kmsId, configId)
                           .filter(EncryptedDataKeys.encryptionType, EncryptionType.AZURE_VAULT)
                           .count(new CountOptions().limit(1));

    if (count > 0) {
      String message =
          "Can not delete the Azure Secrets Manager configuration since there are secrets encrypted with this. "
          + "Please transition your secrets to another secret manager and try again.";
      throw new WingsException(AZURE_KEY_VAULT_OPERATION_ERROR, USER).addParam(REASON_KEY, message);
    }
    AzureVaultConfig azureVaultConfig = wingsPersistence.get(AzureVaultConfig.class, configId);
    Preconditions.checkNotNull(azureVaultConfig, "no Azure vault config found with id " + configId);

    if (isNotEmpty(azureVaultConfig.getSecretKey())) {
      wingsPersistence.delete(EncryptedData.class, azureVaultConfig.getSecretKey());
      logger.info("Deleted encrypted auth token record {} associated with Azure Secrets Manager '{}'",
          azureVaultConfig.getSecretKey(), azureVaultConfig.getName());
    }
    return wingsPersistence.delete(azureVaultConfig);
  }
}
