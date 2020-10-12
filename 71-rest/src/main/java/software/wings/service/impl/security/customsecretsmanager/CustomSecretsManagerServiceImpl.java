package software.wings.service.impl.security.customsecretsmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.RESOURCE_NOT_FOUND;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.persistence.HPersistence.upToOne;
import static io.harness.security.encryption.EncryptionType.CUSTOM;
import static software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType.BASH;
import static software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType.POWERSHELL;
import static software.wings.service.intfc.security.SecretManager.ACCOUNT_ID_KEY;

import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.beans.SecretManagerConfig;
import io.harness.beans.SecretManagerConfig.SecretManagerConfigKeys;
import io.harness.eraro.Level;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.NoResultFoundException;
import io.harness.exception.SecretManagementException;
import io.harness.security.encryption.EncryptedDataParams;
import lombok.NonNull;
import org.mongodb.morphia.query.Query;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript;
import software.wings.service.impl.security.AbstractSecretServiceImpl;
import software.wings.service.intfc.security.CustomSecretsManagerEncryptionService;
import software.wings.service.intfc.security.CustomSecretsManagerService;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@OwnedBy(PL)
public class CustomSecretsManagerServiceImpl extends AbstractSecretServiceImpl implements CustomSecretsManagerService {
  private CustomSecretsManagerShellScriptHelper customSecretsManagerShellScriptHelper;
  private CustomSecretsManagerConnectorHelper customSecretsManagerConnectorHelper;
  private CustomSecretsManagerEncryptionService customSecretsManagerEncryptionService;

  @Inject
  CustomSecretsManagerServiceImpl(CustomSecretsManagerShellScriptHelper customSecretsManagerShellScriptHelper,
      CustomSecretsManagerConnectorHelper customSecretsManagerConnectorHelper,
      CustomSecretsManagerEncryptionService customSecretsManagerEncryptionService) {
    this.customSecretsManagerShellScriptHelper = customSecretsManagerShellScriptHelper;
    this.customSecretsManagerConnectorHelper = customSecretsManagerConnectorHelper;
    this.customSecretsManagerEncryptionService = customSecretsManagerEncryptionService;
  }

  @Override
  public CustomSecretsManagerConfig getSecretsManager(String accountId, String configId) {
    CustomSecretsManagerConfig customSecretsManagerConfig = getSecretsManagerInternal(accountId, configId);
    setAdditionalDetails(customSecretsManagerConfig);
    return customSecretsManagerConfig;
  }

  @Override
  public void setAdditionalDetails(@NonNull CustomSecretsManagerConfig customSecretsManagerConfig) {
    setShellScriptInConfig(customSecretsManagerConfig);
    if (!(customSecretsManagerConfig.isExecuteOnDelegate() || customSecretsManagerConfig.isConnectorTemplatized())) {
      customSecretsManagerConnectorHelper.setConnectorInConfig(customSecretsManagerConfig, new HashSet<>());
    }
  }

  @Override
  public boolean validateSecretsManager(String accountId, @NonNull CustomSecretsManagerConfig secretsManagerConfig) {
    checkIfSecretsManagerConfigCanBeCreatedOrUpdated(accountId);
    secretsManagerConfig.setAccountId(accountId);
    setShellScriptInConfig(secretsManagerConfig);
    setCommandPathInConfig(secretsManagerConfig);
    Set<EncryptedDataParams> testVariables = secretsManagerConfig.getTestVariables();
    if (!secretsManagerConfig.isExecuteOnDelegate()) {
      customSecretsManagerConnectorHelper.setConnectorInConfig(secretsManagerConfig, testVariables);
    }
    validateInternal(secretsManagerConfig, testVariables);
    return true;
  }

  @Override
  public String saveSecretsManager(String accountId, @NonNull CustomSecretsManagerConfig secretsManagerConfig) {
    secretsManagerConfig.setAccountId(accountId);
    upsertSecretsManagerInternal(secretsManagerConfig);
    generateAuditForSecretManager(accountId, null, secretsManagerConfig);
    return secretsManagerConfig.getUuid();
  }

  @Override
  public String updateSecretsManager(String accountId, @NonNull CustomSecretsManagerConfig secretsManagerConfig) {
    secretsManagerConfig.setAccountId(accountId);
    secretsManagerConfig.setEncryptionType(CUSTOM);

    if (isEmpty(secretsManagerConfig.getUuid())) {
      String errorMessage = "Update request for custom secret manager received without the secret manager id";
      throw new InvalidArgumentsException(errorMessage, USER);
    }

    CustomSecretsManagerConfig oldConfig = getSecretsManagerInternal(accountId, secretsManagerConfig.getUuid());
    upsertSecretsManagerInternal(secretsManagerConfig);
    generateAuditForSecretManager(accountId, oldConfig, secretsManagerConfig);
    return secretsManagerConfig.getUuid();
  }

  @Override
  public boolean deleteSecretsManager(String accountId, String configId) {
    long count = wingsPersistence.createQuery(EncryptedData.class)
                     .filter(ACCOUNT_ID_KEY, accountId)
                     .filter(EncryptedDataKeys.kmsId, configId)
                     .filter(EncryptedDataKeys.encryptionType, CUSTOM)
                     .count(upToOne);

    if (count > 0) {
      String message =
          "Can not delete the custom secret manager configuration since there are secrets encrypted with this. "
          + "Please delete your secrets and try again.";
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, message, USER);
    }

    CustomSecretsManagerConfig customSecretsManagerConfig = getSecretsManagerInternal(accountId, configId);
    return deleteSecretManagerAndGenerateAudit(accountId, customSecretsManagerConfig);
  }

  private void upsertSecretsManagerInternal(@NonNull CustomSecretsManagerConfig secretsManagerConfig) {
    secretsManagerConfig.setEncryptionType(CUSTOM);
    setShellScriptInConfig(secretsManagerConfig);
    Set<EncryptedDataParams> testVariables = secretsManagerConfig.getTestVariables();
    if (!secretsManagerConfig.isExecuteOnDelegate()) {
      customSecretsManagerConnectorHelper.setConnectorInConfig(secretsManagerConfig, testVariables);
    }
    setCommandPathInConfig(secretsManagerConfig);
    validateInternal(secretsManagerConfig, testVariables);
    secretsManagerConfig.setRemoteHostConnector(null);
    secretsManagerConfig.setCustomSecretsManagerShellScript(null);
    try {
      String configId = secretManagerConfigService.save(secretsManagerConfig);
      secretsManagerConfig.setUuid(configId);
    } catch (DuplicateKeyException e) {
      String errorMessage = String.format(
          "Could not save the custom secrets manager with name \"%s\". There might be already a secret manager with this name.",
          secretsManagerConfig.getName());
      throw new InvalidArgumentsException(errorMessage, e, USER);
    }
  }

  private void setCommandPathInConfig(CustomSecretsManagerConfig secretsManagerConfig) {
    if (isEmpty(secretsManagerConfig.getCommandPath())) {
      if (secretsManagerConfig.getCustomSecretsManagerShellScript().getScriptType() == BASH
          || secretsManagerConfig.isExecuteOnDelegate()) {
        secretsManagerConfig.setCommandPath("/tmp");
      } else if (secretsManagerConfig.getCustomSecretsManagerShellScript().getScriptType() == POWERSHELL) {
        secretsManagerConfig.setCommandPath("%TEMP%");
      }
    }
  }

  private CustomSecretsManagerConfig getSecretsManagerInternal(String accountId, String configId) {
    Query<SecretManagerConfig> query = wingsPersistence.createQuery(SecretManagerConfig.class)
                                           .field(ID_KEY)
                                           .equal(configId)
                                           .field(SecretManagerConfigKeys.accountId)
                                           .equal(accountId);

    return (CustomSecretsManagerConfig) Optional.ofNullable(query.get()).<NoResultFoundException>orElseThrow(() -> {
      String errorMessage = String.format("Could not find a custom secret manager with configId %s", configId);
      throw NoResultFoundException.newBuilder()
          .message(errorMessage)
          .code(RESOURCE_NOT_FOUND)
          .level(Level.ERROR)
          .reportTargets(USER_SRE)
          .build();
    });
  }

  private void validateInternal(
      CustomSecretsManagerConfig secretsManagerConfig, Set<EncryptedDataParams> testVariables) {
    checkIfSecretsManagerConfigCanBeCreatedOrUpdated(secretsManagerConfig.getAccountId());
    if (secretsManagerConfig.isDefault()) {
      throw new InvalidArgumentsException("Custom secret manager cannot be set as default secret manager", USER);
    }
    CustomSecretsManagerValidationUtils.validateName(secretsManagerConfig.getName());
    CustomSecretsManagerValidationUtils.validateConnectionAttributes(secretsManagerConfig);
    CustomSecretsManagerValidationUtils.validateVariables(secretsManagerConfig, testVariables);
    validateConnectivity(secretsManagerConfig, testVariables);
  }

  private void validateConnectivity(
      CustomSecretsManagerConfig customSecretsManagerConfig, Set<EncryptedDataParams> testVariables) {
    EncryptedData encryptedData = EncryptedData.builder().name("Test Variables").parameters(testVariables).build();
    customSecretsManagerEncryptionService.validateSecret(encryptedData, customSecretsManagerConfig);
  }

  private void setShellScriptInConfig(CustomSecretsManagerConfig customSecretsManagerConfig) {
    CustomSecretsManagerShellScript customSecretsManagerShellScript =
        customSecretsManagerShellScriptHelper.getShellScript(
            customSecretsManagerConfig.getAccountId(), customSecretsManagerConfig.getTemplateId());
    customSecretsManagerConfig.setCustomSecretsManagerShellScript(customSecretsManagerShellScript);
  }
}
