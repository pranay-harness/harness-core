package io.harness.encryptors.managerproxy;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;

import static software.wings.beans.TaskType.FETCH_SECRET;
import static software.wings.beans.TaskType.VALIDATE_SECRET_REFERENCE;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegatetasks.FetchSecretTaskParameters;
import io.harness.delegatetasks.FetchSecretTaskResponse;
import io.harness.delegatetasks.ValidateSecretReferenceTaskParameters;
import io.harness.delegatetasks.ValidateSecretReferenceTaskResponse;
import io.harness.encryptors.DelegateTaskUtils;
import io.harness.exception.SecretManagementException;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;

import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;

public class ManagerEncryptorHelper {
  private final DelegateService delegateService;

  @Inject
  public ManagerEncryptorHelper(DelegateService delegateService) {
    this.delegateService = delegateService;
  }

  public char[] fetchSecretValue(String accountId, EncryptedRecord encryptedRecord, EncryptionConfig encryptionConfig) {
    FetchSecretTaskParameters parameters =
        FetchSecretTaskParameters.builder().encryptedRecord(encryptedRecord).encryptionConfig(encryptionConfig).build();
    DelegateTask delegateTask = DelegateTask.builder()
                                    .data(TaskData.builder()
                                              .async(false)
                                              .taskType(FETCH_SECRET.name())
                                              .parameters(new Object[] {parameters})
                                              .timeout(TaskData.DEFAULT_SYNC_CALL_TIMEOUT)
                                              .build())
                                    .accountId(accountId)
                                    .build();
    try {
      DelegateResponseData delegateResponseData = delegateService.executeTask(delegateTask);
      DelegateTaskUtils.validateDelegateTaskResponse(delegateResponseData);
      if (!(delegateResponseData instanceof FetchSecretTaskResponse)) {
        throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unknown Response from delegate", USER);
      }
      FetchSecretTaskResponse responseData = (FetchSecretTaskResponse) delegateResponseData;
      return responseData.getSecretValue();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      String message = String.format(
          "Interrupted while fetch secret value with encryption config %s", parameters.getEncryptionConfig().getName());
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, message, USER);
    }
  }

  public boolean validateReference(String accountId, ValidateSecretReferenceTaskParameters parameters) {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .data(TaskData.builder()
                                              .async(false)
                                              .taskType(VALIDATE_SECRET_REFERENCE.name())
                                              .parameters(new Object[] {parameters})
                                              .timeout(TaskData.DEFAULT_SYNC_CALL_TIMEOUT)
                                              .build())
                                    .accountId(accountId)
                                    .build();
    try {
      DelegateResponseData delegateResponseData = delegateService.executeTask(delegateTask);
      DelegateTaskUtils.validateDelegateTaskResponse(delegateResponseData);
      if (!(delegateResponseData instanceof ValidateSecretReferenceTaskResponse)) {
        throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unknown Response from delegate", USER);
      }
      ValidateSecretReferenceTaskResponse responseData = (ValidateSecretReferenceTaskResponse) delegateResponseData;
      return responseData.isReferenceValid();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      String message = String.format("Interrupted while validating reference with encryption config %s",
          parameters.getEncryptionConfig().getName());
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, message, USER);
    }
  }
}
