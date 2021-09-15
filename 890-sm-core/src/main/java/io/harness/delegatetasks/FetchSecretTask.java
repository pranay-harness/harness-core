/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.delegatetasks;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.security.encryption.SecretManagerType.CUSTOM;
import static io.harness.security.encryption.SecretManagerType.KMS;
import static io.harness.security.encryption.SecretManagerType.VAULT;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.encryptors.CustomEncryptor;
import io.harness.encryptors.CustomEncryptorsRegistry;
import io.harness.encryptors.KmsEncryptor;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.encryptors.VaultEncryptor;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class FetchSecretTask extends AbstractDelegateRunnableTask {
  @Inject private VaultEncryptorsRegistry vaultEncryptorsRegistry;
  @Inject private KmsEncryptorsRegistry kmsEncryptorsRegistry;
  @Inject private CustomEncryptorsRegistry customEncryptorsRegistry;

  public FetchSecretTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    return run((FetchSecretTaskParameters) parameters[0]);
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    return run((FetchSecretTaskParameters) parameters);
  }

  private FetchSecretTaskResponse run(FetchSecretTaskParameters fetchSecretTaskParameters) {
    EncryptionConfig config = fetchSecretTaskParameters.getEncryptionConfig();
    EncryptedRecord record = fetchSecretTaskParameters.getEncryptedRecord();
    if (KMS == config.getType()) {
      return runKmsTask(record, config);
    }
    if (VAULT == config.getType()) {
      return runVaultTask(record, config);
    }
    if (CUSTOM == config.getType()) {
      return runCustomTask(record, config);
    }
    throw new SecretManagementDelegateException(SECRET_MANAGEMENT_ERROR,
        String.format("Encryptor for fetch secret task for encryption config %s not configured", config.getName()),
        USER);
  }

  private FetchSecretTaskResponse runKmsTask(EncryptedRecord record, EncryptionConfig config) {
    KmsEncryptor kmsEncryptor = kmsEncryptorsRegistry.getKmsEncryptor(config);
    char[] value = kmsEncryptor.fetchSecretValue(config.getAccountId(), record, config);
    return FetchSecretTaskResponse.builder().secretValue(value).build();
  }

  private FetchSecretTaskResponse runVaultTask(EncryptedRecord record, EncryptionConfig config) {
    VaultEncryptor vaultEncryptor = vaultEncryptorsRegistry.getVaultEncryptor(config.getEncryptionType());
    char[] value = vaultEncryptor.fetchSecretValue(config.getAccountId(), record, config);
    return FetchSecretTaskResponse.builder().secretValue(value).build();
  }

  private FetchSecretTaskResponse runCustomTask(EncryptedRecord record, EncryptionConfig config) {
    CustomEncryptor customEncryptor = customEncryptorsRegistry.getCustomEncryptor(config.getEncryptionType());
    char[] value = customEncryptor.fetchSecretValue(config.getAccountId(), record, config);
    return FetchSecretTaskResponse.builder().secretValue(value).build();
  }
}
