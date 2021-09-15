/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.delegatetasks;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.security.encryption.SecretManagerType.CUSTOM;
import static io.harness.security.encryption.SecretManagerType.VAULT;

import io.harness.beans.SecretText;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.encryptors.CustomEncryptor;
import io.harness.encryptors.CustomEncryptorsRegistry;
import io.harness.encryptors.VaultEncryptor;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ValidateSecretReferenceTask extends AbstractDelegateRunnableTask {
  @Inject VaultEncryptorsRegistry vaultEncryptorsRegistry;
  @Inject CustomEncryptorsRegistry customEncryptorsRegistry;

  public ValidateSecretReferenceTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    return run((ValidateSecretReferenceTaskParameters) parameters[0]);
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    return run((ValidateSecretReferenceTaskParameters) parameters);
  }

  private ValidateSecretReferenceTaskResponse run(ValidateSecretReferenceTaskParameters parameters) {
    EncryptionConfig encryptionConfig = parameters.getEncryptionConfig();

    if (encryptionConfig.getType() == VAULT) {
      return runVaultTask(parameters);
    } else if (encryptionConfig.getType() == CUSTOM) {
      return runCustomTask(parameters);
    }

    throw new SecretManagementDelegateException(SECRET_MANAGEMENT_ERROR,
        String.format("Encryptor for validate reference task for encryption config %s not configured",
            encryptionConfig.getName()),
        USER);
  }

  private ValidateSecretReferenceTaskResponse runVaultTask(ValidateSecretReferenceTaskParameters parameters) {
    EncryptedRecord encryptedRecord = parameters.getEncryptedRecord();
    EncryptionConfig encryptionConfig = parameters.getEncryptionConfig();
    VaultEncryptor vaultEncryptor = vaultEncryptorsRegistry.getVaultEncryptor(encryptionConfig.getEncryptionType());
    boolean isReferenceValid = vaultEncryptor.validateReference(encryptionConfig.getAccountId(),
        SecretText.builder().path(encryptedRecord.getPath()).name(encryptedRecord.getName()).build(), encryptionConfig);
    return ValidateSecretReferenceTaskResponse.builder().isReferenceValid(isReferenceValid).build();
  }

  private ValidateSecretReferenceTaskResponse runCustomTask(ValidateSecretReferenceTaskParameters parameters) {
    EncryptedRecord encryptedRecord = parameters.getEncryptedRecord();
    EncryptionConfig encryptionConfig = parameters.getEncryptionConfig();
    CustomEncryptor customEncryptor = customEncryptorsRegistry.getCustomEncryptor(encryptionConfig.getEncryptionType());
    boolean isReferenceValid = customEncryptor.validateReference(
        encryptionConfig.getAccountId(), encryptedRecord.getParameters(), encryptionConfig);
    return ValidateSecretReferenceTaskResponse.builder().isReferenceValid(isReferenceValid).build();
  }
}
