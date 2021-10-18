package io.harness.delegatetasks;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.encryptors.KmsEncryptor;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.encryptors.VaultEncryptor;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.security.encryption.EncryptionConfig;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.apache.commons.lang3.NotImplementedException;

@OwnedBy(PL)
public class ValidateSecretManagerConfigurationTask extends AbstractDelegateRunnableTask {
  @Inject private VaultEncryptorsRegistry vaultEncryptorsRegistry;
  @Inject private KmsEncryptorsRegistry kmsEncryptorsRegistry;

  public ValidateSecretManagerConfigurationTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    ValidateSecretManagerConfigurationTaskParameters validateSecretManagerConfigurationTaskParameters =
        (ValidateSecretManagerConfigurationTaskParameters) parameters;
    EncryptionConfig encryptionConfig = validateSecretManagerConfigurationTaskParameters.getEncryptionConfig();
    switch (encryptionConfig.getType()) {
      case VAULT:
        return runVaultTask(validateSecretManagerConfigurationTaskParameters);
      case KMS:
        return runKmsTask(validateSecretManagerConfigurationTaskParameters);
      default:
        throw new SecretManagementDelegateException(SECRET_MANAGEMENT_ERROR,
            String.format("Encryptor for validate reference task for encryption config %s not configured",
                encryptionConfig.getName()),
            USER);
    }
  }

  private ValidateSecretManagerConfigurationTaskResponse runVaultTask(
      ValidateSecretManagerConfigurationTaskParameters parameters) {
    EncryptionConfig encryptionConfig = parameters.getEncryptionConfig();
    VaultEncryptor vaultEncryptor = vaultEncryptorsRegistry.getVaultEncryptor(encryptionConfig.getEncryptionType());
    boolean isConfigValid =
        vaultEncryptor.validateSecretManagerConfiguration(encryptionConfig.getAccountId(), encryptionConfig);
    return ValidateSecretManagerConfigurationTaskResponse.builder().isConfigurationValid(isConfigValid).build();
  }

  private ValidateSecretManagerConfigurationTaskResponse runKmsTask(
      ValidateSecretManagerConfigurationTaskParameters parameters) {
    EncryptionConfig encryptionConfig = parameters.getEncryptionConfig();
    KmsEncryptor kmsEncryptor = kmsEncryptorsRegistry.getKmsEncryptor(encryptionConfig);
    boolean isConfigValid = kmsEncryptor.validateKmsConfiguration(encryptionConfig.getAccountId(), encryptionConfig);
    return ValidateSecretManagerConfigurationTaskResponse.builder().isConfigurationValid(isConfigValid).build();
  }
}
