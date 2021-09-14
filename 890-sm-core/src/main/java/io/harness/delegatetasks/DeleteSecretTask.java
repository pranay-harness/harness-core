/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.delegatetasks;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.encryptors.VaultEncryptor;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class DeleteSecretTask extends AbstractDelegateRunnableTask {
  @Inject VaultEncryptorsRegistry vaultEncryptorsRegistry;

  public DeleteSecretTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    return run((DeleteSecretTaskParameters) parameters[0]);
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    return run((DeleteSecretTaskParameters) parameters);
  }

  private DeleteSecretTaskResponse run(DeleteSecretTaskParameters deleteSecretTaskParameters) {
    EncryptionConfig encryptionConfig = deleteSecretTaskParameters.getEncryptionConfig();
    EncryptedRecord encryptedRecord = deleteSecretTaskParameters.getExistingRecord();
    VaultEncryptor vaultEncryptor = vaultEncryptorsRegistry.getVaultEncryptor(encryptionConfig.getEncryptionType());
    boolean deleted = vaultEncryptor.deleteSecret(encryptionConfig.getAccountId(), encryptedRecord, encryptionConfig);
    return DeleteSecretTaskResponse.builder().deleted(deleted).build();
  }
}
