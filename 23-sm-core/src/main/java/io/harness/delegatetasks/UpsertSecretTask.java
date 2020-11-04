package io.harness.delegatetasks;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.encryptors.VaultEncryptor;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class UpsertSecretTask extends AbstractDelegateRunnableTask {
  @Inject VaultEncryptorsRegistry vaultEncryptorsRegistry;

  public UpsertSecretTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    return run((UpsertSecretTaskParameters) parameters[0]);
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    return run((UpsertSecretTaskParameters) parameters);
  }

  private UpsertSecretTaskResponse run(UpsertSecretTaskParameters parameters) {
    EncryptedRecord existingRecord = parameters.getExistingRecord();
    EncryptionConfig encryptionConfig = parameters.getEncryptionConfig();

    VaultEncryptor vaultEncryptor = vaultEncryptorsRegistry.getVaultEncryptor(encryptionConfig.getEncryptionType());
    EncryptedRecord encryptedRecord;
    if (parameters.getTaskType() == UpsertSecretTaskType.UPDATE) {
      encryptedRecord = vaultEncryptor.updateSecret(encryptionConfig.getAccountId(), parameters.getName(),
          parameters.getPlaintext(), existingRecord, encryptionConfig);
    } else if (parameters.getTaskType() == UpsertSecretTaskType.CREATE) {
      encryptedRecord = vaultEncryptor.createSecret(
          encryptionConfig.getAccountId(), parameters.getName(), parameters.getPlaintext(), encryptionConfig);
    } else if (parameters.getTaskType() == UpsertSecretTaskType.RENAME) {
      encryptedRecord = vaultEncryptor.renameSecret(
          encryptionConfig.getAccountId(), parameters.getName(), existingRecord, encryptionConfig);
    } else {
      throw new SecretManagementDelegateException(SECRET_MANAGEMENT_ERROR, "Unknown upsert secret task type", USER);
    }
    return UpsertSecretTaskResponse.builder().encryptedRecord(encryptedRecord).build();
  }
}
