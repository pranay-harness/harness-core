/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.delegatetasks;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;

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
    return run((ValidateSecretReferenceTaskParameters) parameters[0]);
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    return run((ValidateSecretManagerConfigurationTaskParameters) parameters);
  }

  private ValidateSecretManagerConfigurationTaskResponse run(
      ValidateSecretManagerConfigurationTaskParameters parameters) {
    EncryptionConfig encryptionConfig = parameters.getEncryptionConfig();
    switch (encryptionConfig.getType()) {
      case VAULT:
        return runVaultTask(parameters);
      case KMS:
        return runKmsTask(parameters);
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
