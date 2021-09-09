package software.wings.service.impl.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EncryptedData;
import io.harness.beans.SecretManagerConfig;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.SecretManagementException;
import io.harness.expression.SecretString;
import io.harness.secretmanagerclient.NGEncryptedDataMetadata;
import io.harness.secretmanagers.SecretManagerConfigService;
import io.harness.security.SimpleEncryption;
import io.harness.security.encryption.EncryptionType;

import software.wings.beans.Account;
import software.wings.beans.Event.Type;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.features.SecretsManagementFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by rsingh on 11/6/17.
 */
@OwnedBy(PL)
@Slf4j
@TargetModule(HarnessModule._890_SM_CORE)
public abstract class AbstractSecretServiceImpl {
  static final String SECRET_MASK = SecretString.SECRET_MASK;
  protected static final String ID_KEY = "_id";

  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected DelegateProxyFactory delegateProxyFactory;
  @Inject protected SecretManagerConfigService secretManagerConfigService;
  @Inject private AccountService accountService;
  @Inject protected AlertService alertService;
  @Inject @Named(SecretsManagementFeature.FEATURE_NAME) private PremiumFeature secretsManagementFeature;
  @Inject private AuditServiceHelper auditServiceHelper;

  static EncryptedData encryptLocal(char[] value) {
    final String encryptionKey = UUID.randomUUID().toString();
    final SimpleEncryption simpleEncryption = new SimpleEncryption(encryptionKey);
    char[] encryptChars = simpleEncryption.encryptChars(value);

    return EncryptedData.builder()
        .encryptionKey(encryptionKey)
        .encryptedValue(encryptChars)
        .encryptionType(EncryptionType.LOCAL)
        .build();
  }

  static NGEncryptedDataMetadata getNgEncryptedDataMetadata(SecretManagerConfig secretManagerConfig) {
    NGEncryptedDataMetadata metadata = null;
    if (secretManagerConfig != null && secretManagerConfig.getNgMetadata() != null) {
      metadata = NGEncryptedDataMetadata.builder()
                     .identifier(secretManagerConfig.getNgMetadata().getIdentifier())
                     .accountIdentifier(secretManagerConfig.getNgMetadata().getAccountIdentifier())
                     .orgIdentifier(secretManagerConfig.getNgMetadata().getOrgIdentifier())
                     .projectIdentifier(secretManagerConfig.getNgMetadata().getProjectIdentifier())
                     .build();
    }
    return metadata;
  }

  static char[] decryptLocal(EncryptedData data) {
    final SimpleEncryption simpleEncryption = new SimpleEncryption(data.getEncryptionKey());
    return simpleEncryption.decryptChars(data.getEncryptedValue());
  }

  char[] decryptKey(char[] key) {
    final EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, new String(key));
    return decryptLocal(encryptedData);
  }

  protected void checkIfSecretsManagerConfigCanBeCreatedOrUpdated(String accountId) {
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

  protected void generateAuditForSecretManager(
      String accountId, SecretManagerConfig oldConfig, SecretManagerConfig newConfig) {
    Type type = oldConfig == null ? Type.CREATE : Type.UPDATE;
    auditServiceHelper.reportForAuditingUsingAccountId(accountId, oldConfig, newConfig, type);
  }

  protected boolean deleteSecretManagerAndGenerateAudit(String accountId, SecretManagerConfig secretManagerConfig) {
    boolean deleted = false;
    if (secretManagerConfig != null) {
      deleted = secretManagerConfigService.delete(accountId, secretManagerConfig);
      if (deleted) {
        auditServiceHelper.reportDeleteForAuditingUsingAccountId(accountId, secretManagerConfig);
      }

      KmsSetupAlert kmsSetupAlert =
          KmsSetupAlert.builder().kmsId(secretManagerConfig.getUuid()).message(secretManagerConfig.getName()).build();
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
