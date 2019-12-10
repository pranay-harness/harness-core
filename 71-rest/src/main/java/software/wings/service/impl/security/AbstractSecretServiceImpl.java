package software.wings.service.impl.security;

import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.expression.SecretString;
import io.harness.security.SimpleEncryption;
import io.harness.security.encryption.EncryptionType;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.Event.Type;
import software.wings.beans.KmsConfig;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.features.SecretsManagementFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManagerConfigService;

import java.util.UUID;

/**
 * Created by rsingh on 11/6/17.
 */
@Slf4j
public abstract class AbstractSecretServiceImpl {
  static final String SECRET_MASK = SecretString.SECRET_MASK;
  static final String REASON_KEY = "reason";
  static final String ID_KEY = "_id";

  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected DelegateProxyFactory delegateProxyFactory;
  @Inject protected SecretManagerConfigService secretManagerConfigService;
  @Inject private KmsService kmsService;
  @Inject private AccountService accountService;
  @Inject private AlertService alertService;
  @Inject @Named(SecretsManagementFeature.FEATURE_NAME) private PremiumFeature secretsManagementFeature;
  @Inject private AuditServiceHelper auditServiceHelper;

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

  void checkIfSecretsManagerConfigCanBeCreatedOrUpdated(String accountId) {
    Account account = accountService.get(accountId);
    if (account.isLocalEncryptionEnabled()) {
      // Reject creation of new Vault secret manager if 'localEncryptionEnabled' account flag is set
      throw new InvalidRequestException(
          "Can't create new secret manager for a LOCAL encryption enabled account!", USER_SRE);
    }

    if (!secretsManagementFeature.isAvailableForAccount(accountId)) {
      throw new InvalidRequestException(String.format("Operation not permitted for account [%s]", accountId), USER);
    }
  }

  void generateAuditForSecretManager(String accountId, SecretManagerConfig oldConfig, SecretManagerConfig newConfig) {
    Type type = oldConfig == null ? Type.CREATE : Type.UPDATE;
    auditServiceHelper.reportForAuditingUsingAccountId(accountId, oldConfig, newConfig, type);
  }

  boolean deleteSecretManagerAndGenerateAudit(String accountId, SecretManagerConfig secretManagerConfig) {
    boolean deleted = false;
    if (secretManagerConfig != null) {
      String secretManagerConfigId = secretManagerConfig.getUuid();
      deleted = wingsPersistence.delete(SecretManagerConfig.class, secretManagerConfigId);
      if (deleted) {
        auditServiceHelper.reportDeleteForAuditingUsingAccountId(accountId, secretManagerConfig);
      }

      KmsSetupAlert kmsSetupAlert =
          KmsSetupAlert.builder().kmsId(secretManagerConfigId).message(secretManagerConfig.getName()).build();
      alertService.closeAlert(accountId, GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
    }
    return deleted;
  }

  static void checkNotNull(Object object, String errorMessage) {
    checkNotNull(object, ErrorCode.SECRET_MANAGEMENT_ERROR, errorMessage);
  }

  static void checkNotNull(Object object, ErrorCode errorCode, String errorMessage) {
    if (object == null) {
      throw new SecretManagementException(errorCode, errorMessage, USER);
    }
  }

  static void checkState(boolean expression, String errorMessage) {
    checkState(expression, ErrorCode.SECRET_MANAGEMENT_ERROR, errorMessage);
  }

  static void checkState(boolean expression, ErrorCode errorCode, String errorMessage) {
    if (!expression) {
      throw new SecretManagementException(errorCode, errorMessage, USER);
    }
  }
}
