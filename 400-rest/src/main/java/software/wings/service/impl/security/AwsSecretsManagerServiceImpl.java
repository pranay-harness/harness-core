package software.wings.service.impl.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.AWS_SECRETS_MANAGER_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.persistence.HPersistence.upToOne;

import static software.wings.settings.SettingVariableTypes.AWS_SECRETS_MANAGER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.beans.EncryptedDataParent;
import io.harness.beans.SecretManagerConfig.SecretManagerConfigKeys;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.exception.SecretManagementException;
import io.harness.security.encryption.EncryptionType;
import io.harness.serializer.KryoSerializer;

import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.AwsSecretsManagerConfig.AwsSecretsManagerConfigKeys;
import software.wings.service.intfc.security.AwsSecretsManagerService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.DuplicateKeyException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@OwnedBy(PL)
@Singleton
@Slf4j
public class AwsSecretsManagerServiceImpl extends AbstractSecretServiceImpl implements AwsSecretsManagerService {
  private static final String SECRET_KEY_NAME_SUFFIX = "_secretKey";
  private static final String AWS_SECRETS_MANAGER_VALIDATION_URL = "aws_secrets_manager_validation";
  @Inject private KryoSerializer kryoSerializer;
  @Inject private VaultEncryptorsRegistry vaultEncryptorsRegistry;

  @Override
  public AwsSecretsManagerConfig getAwsSecretsManagerConfig(String accountId, String configId) {
    Query<AwsSecretsManagerConfig> query = wingsPersistence.createQuery(AwsSecretsManagerConfig.class)
                                               .filter(SecretManagerConfigKeys.accountId, accountId)
                                               .filter(ID_KEY, configId);
    AwsSecretsManagerConfig secretsManagerConfig = query.get();

    if (secretsManagerConfig != null) {
      decryptAsmConfigSecrets(accountId, secretsManagerConfig, false);
    }

    return secretsManagerConfig;
  }

  @Override
  public String saveAwsSecretsManagerConfig(String accountId, AwsSecretsManagerConfig secretsManagerConfig) {
    checkIfSecretsManagerConfigCanBeCreatedOrUpdated(accountId);
    secretsManagerConfig.setAccountId(accountId);

    AwsSecretsManagerConfig oldConfigForAudit = null;
    AwsSecretsManagerConfig savedSecretsManagerConfig = null;
    boolean credentialChanged = true;
    if (!isEmpty(secretsManagerConfig.getUuid())) {
      savedSecretsManagerConfig = getAwsSecretsManagerConfig(accountId, secretsManagerConfig.getUuid());
      if (SECRET_MASK.equals(secretsManagerConfig.getSecretKey())) {
        secretsManagerConfig.setSecretKey(savedSecretsManagerConfig.getSecretKey());
      }
      credentialChanged = isCredentialChanged(secretsManagerConfig, savedSecretsManagerConfig);
      // secret field un-decrypted version of saved AWS config
      savedSecretsManagerConfig = wingsPersistence.get(AwsSecretsManagerConfig.class, secretsManagerConfig.getUuid());
      oldConfigForAudit = kryoSerializer.clone(savedSecretsManagerConfig);
    }

    // Validate every time when secret manager config change submitted
    validateSecretsManagerConfig(accountId, secretsManagerConfig);

    if (!credentialChanged) {
      // update without access/secret key changes
      savedSecretsManagerConfig.setName(secretsManagerConfig.getName());
      savedSecretsManagerConfig.setDefault(secretsManagerConfig.isDefault());
      savedSecretsManagerConfig.setSecretNamePrefix(secretsManagerConfig.getSecretNamePrefix());
      savedSecretsManagerConfig.setScopedToAccount(secretsManagerConfig.isScopedToAccount());
      savedSecretsManagerConfig.setUsageRestrictions(secretsManagerConfig.getUsageRestrictions());

      // PL-3237: Audit secret manager config changes.
      generateAuditForSecretManager(accountId, oldConfigForAudit, savedSecretsManagerConfig);

      return secretManagerConfigService.save(savedSecretsManagerConfig);
    }

    EncryptedData secretKeyEncryptedData = getEncryptedDataForSecretField(
        savedSecretsManagerConfig, secretsManagerConfig, secretsManagerConfig.getSecretKey(), SECRET_KEY_NAME_SUFFIX);

    String secretsManagerConfigId;
    try {
      secretsManagerConfigId = secretManagerConfigService.save(secretsManagerConfig);
    } catch (DuplicateKeyException e) {
      throw new SecretManagementException(AWS_SECRETS_MANAGER_OPERATION_ERROR,
          "Another AWS Secrets Manager configuration with the same name or URL exists", e, USER_SRE);
    }

    if (isEmpty(secretsManagerConfig.getSecretKey())) {
      if (savedSecretsManagerConfig != null && isNotEmpty(savedSecretsManagerConfig.getSecretKey())) {
        wingsPersistence.delete(EncryptedData.class, savedSecretsManagerConfig.getSecretKey());
        log.info("Deleted encrypted auth token record {} associated with Aws Secrets Manager '{}'",
            secretsManagerConfig.getSecretKey(), secretsManagerConfig.getName());
        secretsManagerConfig.setSecretKey(null);
        log.info("Updated secret key referenece as Null for Aws Secrets Manager '{}'", secretsManagerConfig.getName());
      }
    } else {
      // Create a LOCAL encrypted record for AWS secret key
      String secretKeyEncryptedDataId = saveSecretField(secretsManagerConfig, secretsManagerConfigId,
          secretKeyEncryptedData, SECRET_KEY_NAME_SUFFIX, AwsSecretsManagerConfigKeys.secretKey);
      secretsManagerConfig.setSecretKey(secretKeyEncryptedDataId);
    }
    // PL-3237: Audit secret manager config changes.
    generateAuditForSecretManager(accountId, oldConfigForAudit, secretsManagerConfig);

    return secretManagerConfigService.save(secretsManagerConfig);
  }

  private boolean isCredentialChanged(
      AwsSecretsManagerConfig secretsManagerConfig, AwsSecretsManagerConfig savedSecretsManagerConfig) {
    return !Objects.equals(secretsManagerConfig.getAccessKey(), savedSecretsManagerConfig.getAccessKey())
        || !Objects.equals(secretsManagerConfig.getSecretKey(), savedSecretsManagerConfig.getSecretKey())
        || !Objects.equals(
            secretsManagerConfig.isAssumeIamRoleOnDelegate(), savedSecretsManagerConfig.isAssumeIamRoleOnDelegate())
        || !Objects.equals(
            secretsManagerConfig.isAssumeStsRoleOnDelegate(), savedSecretsManagerConfig.isAssumeStsRoleOnDelegate())
        || !Objects.equals(secretsManagerConfig.getRoleArn(), savedSecretsManagerConfig.getRoleArn())
        || !Objects.equals(secretsManagerConfig.getExternalName(), savedSecretsManagerConfig.getExternalName())
        || !Objects.equals(
            secretsManagerConfig.getAssumeStsRoleDuration(), savedSecretsManagerConfig.getAssumeStsRoleDuration())
        || !Objects.equals(
            secretsManagerConfig.getDelegateSelectors(), savedSecretsManagerConfig.getDelegateSelectors());
  }

  @Override
  public void validateSecretsManagerConfig(String accountId, AwsSecretsManagerConfig secretsManagerConfig) {
    vaultEncryptorsRegistry.getVaultEncryptor(EncryptionType.AWS_SECRETS_MANAGER)
        .validateSecretManagerConfiguration(accountId, secretsManagerConfig);
  }

  @Override
  public void decryptAsmConfigSecrets(
      String accountId, AwsSecretsManagerConfig secretsManagerConfig, boolean maskSecret) {
    if (maskSecret) {
      secretsManagerConfig.maskSecrets();
    } else {
      if (!secretsManagerConfig.isAssumeStsRoleOnDelegate() && !secretsManagerConfig.isAssumeIamRoleOnDelegate()) {
        EncryptedData encryptedSecretKey =
            wingsPersistence.get(EncryptedData.class, secretsManagerConfig.getSecretKey());
        checkNotNull(
            encryptedSecretKey, "Secret key can't be null for AWS Secrets Manager " + secretsManagerConfig.getUuid());
        secretsManagerConfig.setSecretKey(String.valueOf(decryptLocal(encryptedSecretKey)));
      }
    }
  }

  private EncryptedData getEncryptedDataForSecretField(AwsSecretsManagerConfig savedSecretsManagerConfig,
      AwsSecretsManagerConfig secretsManagerConfig, String secretValue, String secretNameSuffix) {
    EncryptedData encryptedData = isNotEmpty(secretValue) ? encryptLocal(secretValue.toCharArray()) : null;
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

  private String saveSecretField(AwsSecretsManagerConfig secretsManagerConfig, String configId,
      EncryptedData secretFieldEncryptedData, String secretNameSuffix, String fieldName) {
    String secretFieldEncryptedDataId = null;
    if (secretFieldEncryptedData != null) {
      secretFieldEncryptedData.setAccountId(secretsManagerConfig.getAccountId());
      secretFieldEncryptedData.addParent(
          EncryptedDataParent.createParentRef(configId, AwsSecretsManagerConfig.class, fieldName, AWS_SECRETS_MANAGER));
      secretFieldEncryptedData.setType(AWS_SECRETS_MANAGER);
      secretFieldEncryptedData.setName(secretsManagerConfig.getName() + secretNameSuffix);
      secretFieldEncryptedDataId = wingsPersistence.save(secretFieldEncryptedData);
    }
    return secretFieldEncryptedDataId;
  }

  @Override
  public boolean deleteAwsSecretsManagerConfig(String accountId, String configId) {
    long count = wingsPersistence.createQuery(EncryptedData.class)
                     .filter(EncryptedDataKeys.accountId, accountId)
                     .filter(EncryptedDataKeys.kmsId, configId)
                     .filter(EncryptedDataKeys.encryptionType, EncryptionType.AWS_SECRETS_MANAGER)
                     .count(upToOne);

    if (count > 0) {
      String message = "Cannot delete the AWS Secrets Manager configuration since there are secrets encrypted with it. "
          + "Please transition your secrets to another secret manager and try again.";
      throw new SecretManagementException(AWS_SECRETS_MANAGER_OPERATION_ERROR, message, USER);
    }

    AwsSecretsManagerConfig secretsManagerConfig = wingsPersistence.get(AwsSecretsManagerConfig.class, configId);
    checkNotNull(secretsManagerConfig, "No Aws Secrets Manager configuration found with id " + configId);

    if (isNotEmpty(secretsManagerConfig.getSecretKey())) {
      wingsPersistence.delete(EncryptedData.class, secretsManagerConfig.getSecretKey());
      log.info("Deleted encrypted auth token record {} associated with Aws Secrets Manager '{}'",
          secretsManagerConfig.getSecretKey(), secretsManagerConfig.getName());
    }

    return deleteSecretManagerAndGenerateAudit(accountId, secretsManagerConfig);
  }
}
