package software.wings.service.impl.security.customsecretsmanager;

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

import io.harness.eraro.Level;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.NoResultFoundException;
import io.harness.exception.UnexpectedException;
import io.harness.security.encryption.SecretVariable;
import lombok.NonNull;
import org.mongodb.morphia.query.Query;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.SecretManagerConfig.SecretManagerConfigKeys;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig.CustomSecretsManagerConfigKeys;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType;
import software.wings.service.impl.security.AbstractSecretServiceImpl;
import software.wings.service.impl.security.SecretManagementException;
import software.wings.service.intfc.security.CustomSecretsManagerService;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class CustomSecretsManagerServiceImpl extends AbstractSecretServiceImpl implements CustomSecretsManagerService {
  private CustomSecretsManagerShellScriptHelper customSecretsManagerShellScriptHelper;
  private CustomSecretsManagerConnectorHelper customSecretsManagerConnectorHelper;

  @Inject
  CustomSecretsManagerServiceImpl(CustomSecretsManagerShellScriptHelper customSecretsManagerShellScriptHelper,
      CustomSecretsManagerConnectorHelper customSecretsManagerConnectorHelper) {
    this.customSecretsManagerShellScriptHelper = customSecretsManagerShellScriptHelper;
    this.customSecretsManagerConnectorHelper = customSecretsManagerConnectorHelper;
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
      setConnectorInConfig(customSecretsManagerConfig, new HashSet<>());
    }
  }

  @Override
  public boolean validateSecretsManager(String accountId, @NonNull CustomSecretsManagerConfig secretsManagerConfig) {
    checkIfSecretsManagerConfigCanBeCreatedOrUpdated(accountId);
    secretsManagerConfig.setAccountId(accountId);
    setShellScriptInConfig(secretsManagerConfig);
    setCommandPathInConfig(secretsManagerConfig);
    Set<SecretVariable> testVariables = secretsManagerConfig.getTestVariables();
    if (!secretsManagerConfig.isExecuteOnDelegate()) {
      setConnectorInConfig(secretsManagerConfig, testVariables);
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
    Set<SecretVariable> testVariables = secretsManagerConfig.getTestVariables();
    if (!secretsManagerConfig.isExecuteOnDelegate()) {
      setConnectorInConfig(secretsManagerConfig, testVariables);
    }
    setCommandPathInConfig(secretsManagerConfig);
    validateInternal(secretsManagerConfig, testVariables);
    String configId =
        Optional.ofNullable(secretManagerConfigService.save(secretsManagerConfig)).<UnexpectedException>orElseThrow(() -> {
          String errorMessage =
              "Could not save the custom secrets manager to the db for account %s with name %s. There might be already a secret manager with this name.";
          throw new UnexpectedException(errorMessage);
        });
    secretsManagerConfig.setUuid(configId);
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

  private void validateInternal(CustomSecretsManagerConfig secretsManagerConfig, Set<SecretVariable> testVariables) {
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
      CustomSecretsManagerConfig customSecretsManagerConfig, Set<SecretVariable> testVariables) {
    // To be implemented
  }

  private void setShellScriptInConfig(CustomSecretsManagerConfig customSecretsManagerConfig) {
    CustomSecretsManagerShellScript customSecretsManagerShellScript =
        customSecretsManagerShellScriptHelper.getShellScript(
            customSecretsManagerConfig.getAccountId(), customSecretsManagerConfig.getTemplateId());
    customSecretsManagerConfig.setCustomSecretsManagerShellScript(customSecretsManagerShellScript);
  }

  private void setConnectorInConfig(
      CustomSecretsManagerConfig customSecretsManagerConfig, Set<SecretVariable> testVariables) {
    String accountId = customSecretsManagerConfig.getAccountId();
    String connectorId = customSecretsManagerConfig.getConnectorId();
    ScriptType scriptType = customSecretsManagerConfig.getCustomSecretsManagerShellScript().getScriptType();

    if (customSecretsManagerConfig.isConnectorTemplatized()) {
      connectorId = getConnectorIdFromTestVariables(testVariables);
    }
    EncryptableSetting remoteHostConnector =
        customSecretsManagerConnectorHelper.getConnector(accountId, connectorId, scriptType);
    customSecretsManagerConfig.setRemoteHostConnector(remoteHostConnector);
  }

  private static String getConnectorIdFromTestVariables(Set<SecretVariable> testVariables) {
    return testVariables.stream()
        .filter(secretVariable -> secretVariable.getName().equals(CustomSecretsManagerConfigKeys.connectorId))
        .findFirst()
        .<InvalidArgumentsException>orElseThrow(() -> {
          String errorMessage = "There is no connector supplied as a variable although the connector was templatized";
          throw new InvalidArgumentsException(errorMessage, USER);
        })
        .getValue();
  }
}
