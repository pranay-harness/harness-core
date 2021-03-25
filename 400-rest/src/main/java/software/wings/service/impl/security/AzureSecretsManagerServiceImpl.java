package software.wings.service.impl.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;
import static io.harness.eraro.ErrorCode.AZURE_KEY_VAULT_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.persistence.HPersistence.upToOne;

import static software.wings.settings.SettingVariableTypes.AZURE_VAULT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.beans.EncryptedDataParent;
import io.harness.exception.AzureKeyVaultOperationException;
import io.harness.exception.SecretManagementException;
import io.harness.security.encryption.EncryptionType;
import io.harness.serializer.KryoSerializer;

import software.wings.beans.AzureVaultConfig;
import software.wings.beans.AzureVaultConfig.AzureVaultConfigKeys;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.service.intfc.security.AzureSecretsManagerService;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.azure.management.keyvault.Vault;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasName;
import com.mongodb.DuplicateKeyException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@OwnedBy(PL)
@Singleton
@Slf4j
public class AzureSecretsManagerServiceImpl extends AbstractSecretServiceImpl implements AzureSecretsManagerService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AzureHelperService azureHelperService;
  @Inject private KryoSerializer kryoSerializer;
  private static final String SECRET_KEY_NAME_SUFFIX = "_secretKey";

  @Override
  public String saveAzureSecretsManagerConfig(String accountId, AzureVaultConfig azureVautConfig) {
    checkIfSecretsManagerConfigCanBeCreatedOrUpdated(accountId);
    validateConfig(azureVautConfig);
    azureVautConfig.setAccountId(accountId);
    AzureVaultConfig oldConfigForAudit = null;
    AzureVaultConfig savedAzureVaultConfig = null;

    boolean updateCallWithMaskedSecretKey = false;

    if (hasSome(azureVautConfig.getUuid())) {
      savedAzureVaultConfig = wingsPersistence.get(AzureVaultConfig.class, azureVautConfig.getUuid());
      oldConfigForAudit = kryoSerializer.clone(savedAzureVaultConfig);

      updateCallWithMaskedSecretKey = SECRET_MASK.equals(azureVautConfig.getSecretKey());
    }

    if (updateCallWithMaskedSecretKey) {
      azureVautConfig.setSecretKey(savedAzureVaultConfig.getSecretKey());
      azureVautConfig.setUuid(savedAzureVaultConfig.getUuid());

      // PL-3237: Audit secret manager config changes.
      generateAuditForSecretManager(accountId, oldConfigForAudit, azureVautConfig);

      return secretManagerConfigService.save(azureVautConfig);
    }

    EncryptedData secretKeyEncryptedData = getEncryptedDataForSecretField(
        azureVautConfig, azureVautConfig, azureVautConfig.getSecretKey(), SECRET_KEY_NAME_SUFFIX);
    azureVautConfig.setSecretKey(null);
    String secretsManagerConfigId;

    try {
      secretsManagerConfigId = secretManagerConfigService.save(azureVautConfig);
    } catch (DuplicateKeyException e) {
      throw new AzureKeyVaultOperationException(
          "Another Azure vault secret configuration with the same name or URL exists", AZURE_KEY_VAULT_OPERATION_ERROR,
          USER_SRE);
    }

    // Create a LOCAL encrypted record for Azure secret key
    String secretKeyEncryptedDataId = saveSecretField(azureVautConfig, secretsManagerConfigId, secretKeyEncryptedData,
        SECRET_KEY_NAME_SUFFIX, AzureVaultConfigKeys.secretKey);
    azureVautConfig.setSecretKey(secretKeyEncryptedDataId);

    // PL-3237: Audit secret manager config changes.
    generateAuditForSecretManager(accountId, oldConfigForAudit, azureVautConfig);

    return secretManagerConfigService.save(azureVautConfig);
  }

  private void validateConfig(AzureVaultConfig azureVautConfig) {
    if (hasNone(azureVautConfig.getName())) {
      throw new SecretManagementException(AZURE_KEY_VAULT_OPERATION_ERROR, "Name can not be empty", USER);
    }
  }

  private String saveSecretField(AzureVaultConfig secretsManagerConfig, String configId,
      EncryptedData secretFieldEncryptedData, String secretNameSuffix, String fieldName) {
    String secretFieldEncryptedDataId = null;
    if (secretFieldEncryptedData != null) {
      secretFieldEncryptedData.setAccountId(secretsManagerConfig.getAccountId());
      secretFieldEncryptedData.addParent(
          EncryptedDataParent.createParentRef(configId, AzureVaultConfig.class, fieldName, AZURE_VAULT));
      secretFieldEncryptedData.setType(AZURE_VAULT);
      secretFieldEncryptedData.setName(secretsManagerConfig.getName() + secretNameSuffix);
      secretFieldEncryptedDataId = wingsPersistence.save(secretFieldEncryptedData);
    }
    return secretFieldEncryptedDataId;
  }

  private EncryptedData getEncryptedDataForSecretField(AzureVaultConfig savedSecretsManagerConfig,
      AzureVaultConfig secretsManagerConfig, String secretValue, String secretNameSuffix) {
    EncryptedData encryptedData = hasSome(secretValue) ? encryptLocal(secretValue.toCharArray()) : null;
    if (savedSecretsManagerConfig != null && encryptedData != null) {
      // Get by auth token encrypted record by Id or name.
      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
      query.criteria(EncryptedDataKeys.accountId)
          .equal(secretsManagerConfig.getAccountId())
          .or(query.criteria(ID_KEY).equal(secretsManagerConfig.getSecretKey()),
              query.criteria(EncryptedDataKeys.name).equal(secretsManagerConfig.getName() + secretNameSuffix));
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

  @Override
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

  @Override
  public AzureVaultConfig getEncryptionConfig(String accountId, String id) {
    AzureVaultConfig secretsManagerConfig = wingsPersistence.get(AzureVaultConfig.class, id);
    Preconditions.checkNotNull(
        secretsManagerConfig, String.format("Azure vault config not found for id: %s in account: %s", id, accountId));
    decryptAzureConfigSecrets(secretsManagerConfig, false);
    return secretsManagerConfig;
  }

  @Override
  public boolean deleteConfig(String accountId, String configId) {
    long count = wingsPersistence.createQuery(EncryptedData.class)
                     .filter(EncryptedDataKeys.accountId, accountId)
                     .filter(EncryptedDataKeys.kmsId, configId)
                     .filter(EncryptedDataKeys.encryptionType, EncryptionType.AZURE_VAULT)
                     .count(upToOne);

    if (count > 0) {
      String message =
          "Cannot delete the Azure Secrets Manager configuration since there are secrets encrypted with it. "
          + "Please transition your secrets to another secret manager and try again.";
      throw new AzureKeyVaultOperationException(message, AZURE_KEY_VAULT_OPERATION_ERROR, USER);
    }
    AzureVaultConfig azureVaultConfig = wingsPersistence.get(AzureVaultConfig.class, configId);
    Preconditions.checkNotNull(azureVaultConfig, "no Azure vault config found with id " + configId);

    if (hasSome(azureVaultConfig.getSecretKey())) {
      wingsPersistence.delete(EncryptedData.class, azureVaultConfig.getSecretKey());
      log.info("Deleted encrypted auth token record {} associated with Azure Secrets Manager '{}'",
          azureVaultConfig.getSecretKey(), azureVaultConfig.getName());
    }
    return deleteSecretManagerAndGenerateAudit(accountId, azureVaultConfig);
  }
}
