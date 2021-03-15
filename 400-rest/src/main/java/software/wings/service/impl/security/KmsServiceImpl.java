package software.wings.service.impl.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.beans.EncryptedData.PARENT_ID_KEY;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.KMS_OPERATION_ERROR;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.persistence.HPersistence.upToOne;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.settings.SettingVariableTypes.KMS;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.beans.EncryptedDataParent;
import io.harness.beans.SecretManagerConfig.SecretManagerConfigKeys;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.exception.SecretManagementException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptionType;
import io.harness.serializer.KryoSerializer;

import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.KmsConfig.KmsConfigKeys;
import software.wings.service.intfc.security.KmsService;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

/**
 * Created by rsingh on 9/29/17.
 */
@OwnedBy(PL)
@Singleton
@Slf4j
public class KmsServiceImpl extends AbstractSecretServiceImpl implements KmsService {
  public static final String ACCESS_KEY_SUFFIX = "_accessKey";
  public static final String SECRET_KEY_SUFFIX = "_secretKey";
  public static final String ARN_SUFFIX = "_arn";
  @Inject private KryoSerializer kryoSerializer;
  @Inject private KmsEncryptorsRegistry kmsEncryptorsRegistry;

  @Override
  public String saveGlobalKmsConfig(String accountId, KmsConfig kmsConfig) {
    return saveKmsConfigInternal(GLOBAL_ACCOUNT_ID, kmsConfig);
  }

  @Override
  public KmsConfig getGlobalKmsConfig() {
    KmsConfig globalKmsConfig = wingsPersistence.createQuery(KmsConfig.class)
                                    .field(SecretManagerConfigKeys.accountId)
                                    .equal(GLOBAL_ACCOUNT_ID)
                                    .field(SecretManagerConfigKeys.encryptionType)
                                    .equal(EncryptionType.KMS)
                                    .get();
    if (globalKmsConfig == null) {
      return null;
    }
    // Secrets field of raw KmsConfig are encrypted record IDs. It needs to be decrypted to be used.
    decryptKmsConfigSecrets(globalKmsConfig);
    return globalKmsConfig;
  }

  @Override
  public String saveKmsConfig(String accountId, KmsConfig kmsConfig) {
    checkIfSecretsManagerConfigCanBeCreatedOrUpdated(accountId);
    return saveKmsConfigInternal(accountId, kmsConfig);
  }

  private String saveKmsConfigInternal(String accountId, KmsConfig kmsConfig) {
    kmsConfig.setAccountId(accountId);
    KmsConfig oldConfigForAudit = null;
    KmsConfig savedKmsConfig = null;
    boolean credentialChanged = true;
    if (isNotEmpty(kmsConfig.getUuid())) {
      savedKmsConfig = getKmsConfig(accountId, kmsConfig.getUuid());
      // Replaced masked secrets with the real secret value.
      if (SECRET_MASK.equals(kmsConfig.getSecretKey())) {
        kmsConfig.setSecretKey(savedKmsConfig.getSecretKey());
      }
      if (SECRET_MASK.equals(kmsConfig.getKmsArn())) {
        kmsConfig.setKmsArn(savedKmsConfig.getKmsArn());
      }
      credentialChanged = isCredentialChanged(kmsConfig, savedKmsConfig);

      // secret field un-decrypted version of saved KMS config
      savedKmsConfig = wingsPersistence.get(KmsConfig.class, kmsConfig.getUuid());
      oldConfigForAudit = kryoSerializer.clone(savedKmsConfig);
    }

    // Validate every time when secret manager config change submitted
    validateKms(accountId, kmsConfig);

    if (!credentialChanged) {
      savedKmsConfig.setName(kmsConfig.getName());
      savedKmsConfig.setDefault(kmsConfig.isDefault());
      savedKmsConfig.setScopedToAccount(kmsConfig.isScopedToAccount());
      savedKmsConfig.setUsageRestrictions(kmsConfig.getUsageRestrictions());
      // PL-3237: Audit secret manager config changes.
      generateAuditForSecretManager(accountId, oldConfigForAudit, savedKmsConfig);
      return secretManagerConfigService.save(savedKmsConfig);
    }

    EncryptedData accessKeyData = getEncryptedDataForSecretField(
        savedKmsConfig, kmsConfig, kmsConfig.getAccessKey(), savedKmsConfig.getAccessKey(), ACCESS_KEY_SUFFIX);
    EncryptedData secretKeyData = getEncryptedDataForSecretField(
        savedKmsConfig, kmsConfig, kmsConfig.getSecretKey(), savedKmsConfig.getSecretKey(), SECRET_KEY_SUFFIX);
    EncryptedData arnKeyData = getEncryptedDataForSecretField(
        savedKmsConfig, kmsConfig, kmsConfig.getKmsArn(), savedKmsConfig.getKmsArn(), ARN_SUFFIX);
    accessKeyData = saveOrUpdateEncryptedRecord(accountId, kmsConfig, kmsConfig.getAccessKey(),
        savedKmsConfig.getAccessKey(), accessKeyData, ACCESS_KEY_SUFFIX);
    secretKeyData = saveOrUpdateEncryptedRecord(accountId, kmsConfig, kmsConfig.getSecretKey(),
        savedKmsConfig.getSecretKey(), secretKeyData, SECRET_KEY_SUFFIX);
    arnKeyData = saveOrUpdateEncryptedRecord(
        accountId, kmsConfig, kmsConfig.getKmsArn(), savedKmsConfig.getKmsArn(), arnKeyData, ARN_SUFFIX);
    kmsConfig.setAccessKey(accessKeyData.getUuid());
    kmsConfig.setSecretKey(secretKeyData.getUuid());
    kmsConfig.setKmsArn(arnKeyData.getUuid());
    // PL-3237: Audit secret manager config changes.
    generateAuditForSecretManager(accountId, oldConfigForAudit, kmsConfig);
    String parentId = secretManagerConfigService.save(kmsConfig);
    if (accessKeyData != null) {
      accessKeyData.addParent(
          EncryptedDataParent.createParentRef(parentId, KmsConfig.class, KmsConfigKeys.accessKey, KMS));
      wingsPersistence.save(accessKeyData);
    }
    if (secretKeyData != null) {
      secretKeyData.addParent(
          EncryptedDataParent.createParentRef(parentId, KmsConfig.class, KmsConfigKeys.secretKey, KMS));
      wingsPersistence.save(secretKeyData);
    }
    if (arnKeyData != null) {
      arnKeyData.addParent(EncryptedDataParent.createParentRef(parentId, KmsConfig.class, KmsConfigKeys.kmsArn, KMS));
      wingsPersistence.save(arnKeyData);
    }
    return parentId;
  }

  private EncryptedData saveOrUpdateEncryptedRecord(String accountId, KmsConfig kmsConfig, String newKeyToUpdate,
      String oldKeyId, EncryptedData encryptedData, String keySuffix) {
    //if there is a key to update save the encrypted data
    if (isNotEmpty(newKeyToUpdate) && encryptedData != null) {
      encryptedData.setAccountId(accountId);
      encryptedData.setType(KMS);
      encryptedData.setName(kmsConfig.getName() + keySuffix);
      wingsPersistence.save(encryptedData);
      return encryptedData;
    } else {
      //if key is not sent that means it needs to be removed from EncryptedRecords as well
      cleanUpEncryptedRecordEntry(accountId, oldKeyId);
      return null;
    }
  }

  private void cleanUpEncryptedRecordEntry(String accountId, String keyIdToDelete) {
    if (isNotEmpty(keyIdToDelete)) {
      wingsPersistence.delete(accountId, EncryptedData.class, keyIdToDelete);
    }
  }

  private boolean isCredentialChanged(KmsConfig kmsConfig, KmsConfig savedKmsConfig) {
    return !Objects.equals(kmsConfig.getRegion(), savedKmsConfig.getRegion())
        || !Objects.equals(kmsConfig.getAccessKey(), savedKmsConfig.getAccessKey())
        || !Objects.equals(kmsConfig.getSecretKey(), savedKmsConfig.getSecretKey())
        || !Objects.equals(kmsConfig.getKmsArn(), savedKmsConfig.getKmsArn())
        || !Objects.equals(kmsConfig.isAssumeIamRoleOnDelegate(), savedKmsConfig.isAssumeIamRoleOnDelegate())
        || !Objects.equals(kmsConfig.isAssumeStsRoleOnDelegate(), savedKmsConfig.isAssumeStsRoleOnDelegate())
        || !Objects.equals(kmsConfig.getRoleArn(), savedKmsConfig.getRoleArn())
        || !Objects.equals(kmsConfig.getExternalName(), savedKmsConfig.getExternalName())
        || !Objects.equals(kmsConfig.getAssumeStsRoleDuration(), savedKmsConfig.getAssumeStsRoleDuration())
        || !Objects.equals(kmsConfig.getDelegateSelectors(), savedKmsConfig.getDelegateSelectors());
  }

  @Override
  public boolean deleteKmsConfig(String accountId, String kmsConfigId) {
    KmsConfig kmsConfig = wingsPersistence.get(KmsConfig.class, kmsConfigId);
    checkNotNull(kmsConfig, "No KMS secret manager found with id " + kmsConfigId);

    if (GLOBAL_ACCOUNT_ID.equals(kmsConfig.getAccountId())) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Can not delete global KMS secret manager", USER);
    }

    long count = wingsPersistence.createQuery(EncryptedData.class)
                     .filter(EncryptedDataKeys.accountId, accountId)
                     .filter(EncryptedDataKeys.kmsId, kmsConfigId)
                     .filter(EncryptedDataKeys.encryptionType, EncryptionType.KMS)
                     .count(upToOne);

    if (count > 0) {
      String message = "Cannot delete the kms configuration since there are secrets encrypted with it. "
          + "Please transition your secrets to another secret manager and try again.";
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, message, USER_SRE);
    }

    Query<EncryptedData> deleteQuery =
        wingsPersistence.createQuery(EncryptedData.class).field(PARENT_ID_KEY).hasThisOne(kmsConfigId);
    wingsPersistence.delete(deleteQuery);

    return deleteSecretManagerAndGenerateAudit(accountId, kmsConfig);
  }

  @Override
  public void decryptKmsConfigSecrets(String accountId, KmsConfig kmsConfig, boolean maskSecret) {
    if (isNotEmpty(kmsConfig.getAccessKey())) {
      EncryptedData accessKeyData = wingsPersistence.get(EncryptedData.class, kmsConfig.getAccessKey());
      // checkNotNull(accessKeyData, "Access key reference is null for KMS secret manager " + kmsConfig.getUuid());
      kmsConfig.setAccessKey(new String(decryptLocal(accessKeyData)));
    }
    if (maskSecret) {
      kmsConfig.maskSecrets();
    } else {
      if (isNotEmpty(kmsConfig.getSecretKey())) {
        EncryptedData secretData = wingsPersistence.get(EncryptedData.class, kmsConfig.getSecretKey());
        // checkNotNull(secretData, "Secret Key reference is null for KMS secret manager " + kmsConfig.getUuid());
        kmsConfig.setSecretKey(new String(decryptLocal(secretData)));
      }
      EncryptedData arnData = wingsPersistence.get(EncryptedData.class, kmsConfig.getKmsArn());
      checkNotNull(arnData, "ARN reference is null for KMS secret manager " + kmsConfig.getUuid());
      kmsConfig.setKmsArn(new String(decryptLocal(arnData)));
    }
  }

  private void validateKms(String accountId, KmsConfig kmsConfig) {
    if (isEmpty(kmsConfig.getName())) {
      throw new SecretManagementException(KMS_OPERATION_ERROR, "Name can not be empty", USER);
    }
    try {
      kmsEncryptorsRegistry.getKmsEncryptor(kmsConfig).encryptSecret(
          accountId, UUID.randomUUID().toString(), kmsConfig);
    } catch (WingsException e) {
      String message = "Was not able to encrypt using given credentials. Please check your credentials and try again";
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, message + e.getMessage(), USER);
    }
  }

  @Override
  public KmsConfig getKmsConfig(String accountId, String entityId) {
    KmsConfig kmsConfig = wingsPersistence.createQuery(KmsConfig.class)
                              .field("accountId")
                              .in(Lists.newArrayList(accountId, GLOBAL_ACCOUNT_ID))
                              .filter("_id", entityId)
                              .get();

    // Secrets field of raw KmsConfig are encrypted record IDs. It needs to be decrypted to be used.
    decryptKmsConfigSecrets(kmsConfig);

    return kmsConfig;
  }

  private void decryptKmsConfigSecrets(KmsConfig kmsConfig) {
    if (kmsConfig != null) {
      if (isNotEmpty(kmsConfig.getAccessKey())) {
        kmsConfig.setAccessKey(new String(decryptKey(kmsConfig.getAccessKey().toCharArray())));
      }
      if (isNotEmpty(kmsConfig.getSecretKey())) {
        kmsConfig.setSecretKey(new String(decryptKey(kmsConfig.getSecretKey().toCharArray())));
      }
      if (isNotEmpty(kmsConfig.getKmsArn())) {
        kmsConfig.setKmsArn(new String(decryptKey(kmsConfig.getKmsArn().toCharArray())));
      }
    }
  }

  private EncryptedData getEncryptedDataForSecretField(KmsConfig savedKmsConfig, KmsConfig kmsConfig,
      String newSecretValue, String oldSecretValue, String secretNameSuffix) {
    EncryptedData encryptedData = isNotEmpty(newSecretValue) ? encryptLocal(newSecretValue.toCharArray()) : null;
    if (savedKmsConfig != null && encryptedData != null) {
      // Get by auth token encrypted record by Id or name.
      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
      query.criteria(EncryptedDataKeys.accountId)
          .equal(kmsConfig.getAccountId())
          .or(query.criteria(ID_KEY).equal(oldSecretValue),
              query.criteria(EncryptedDataKeys.name).equal(kmsConfig.getName() + secretNameSuffix));
      EncryptedData savedEncryptedData = query.get();
      if (savedEncryptedData != null) {
        savedEncryptedData.setEncryptionKey(encryptedData.getEncryptionKey());
        savedEncryptedData.setEncryptedValue(encryptedData.getEncryptedValue());
        encryptedData = savedEncryptedData;
      }
    }
    return encryptedData;
  }
}
