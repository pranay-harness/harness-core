package io.harness.delegatetasks;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.encryptors.KmsEncryptor;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class EncryptSecretTask extends AbstractDelegateRunnableTask {
  @Inject KmsEncryptorsRegistry kmsEncryptorsRegistry;

  public EncryptSecretTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    return run((EncryptSecretTaskParameters) parameters[0]);
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    return run((EncryptSecretTaskParameters) parameters);
  }

  private EncryptSecretTaskResponse run(EncryptSecretTaskParameters deleteSecretTaskParameters) {
    EncryptionConfig encryptionConfig = deleteSecretTaskParameters.getEncryptionConfig();
    String value = deleteSecretTaskParameters.getValue();
    KmsEncryptor kmsEncryptor = kmsEncryptorsRegistry.getKmsEncryptor(encryptionConfig);
    EncryptedRecord encryptedRecord =
        kmsEncryptor.encryptSecret(encryptionConfig.getAccountId(), value, encryptionConfig);
    return EncryptSecretTaskResponse.builder().encryptedRecord(encryptedRecord).build();
  }
}
