package io.harness.ng.core.encryptors;

import com.google.inject.Inject;
import io.harness.beans.DelegateTaskRequest;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegatetasks.FetchSecretTaskParameters;
import io.harness.delegatetasks.FetchSecretTaskResponse;
import io.harness.delegatetasks.ValidateSecretManagerConfigurationTaskParameters;
import io.harness.delegatetasks.ValidateSecretManagerConfigurationTaskResponse;
import io.harness.delegatetasks.ValidateSecretReferenceTaskParameters;
import io.harness.delegatetasks.ValidateSecretReferenceTaskResponse;
import io.harness.encryptors.DelegateTaskUtils;
import io.harness.exception.SecretManagementException;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.service.DelegateGrpcClientWrapper;

import java.time.Duration;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.TaskType.FETCH_SECRET;
import static software.wings.beans.TaskType.VALIDATE_SECRET_MANAGER_CONFIGURATION;
import static software.wings.beans.TaskType.VALIDATE_SECRET_REFERENCE;

public class NGManagerEncryptorHelper {
    private final DelegateGrpcClientWrapper delegateService;

    @Inject
    public NGManagerEncryptorHelper(DelegateGrpcClientWrapper delegateService) {
        this.delegateService = delegateService;
    }

    public char[] fetchSecretValue(String accountId, EncryptedRecord encryptedRecord, EncryptionConfig encryptionConfig) {
        FetchSecretTaskParameters parameters =
                FetchSecretTaskParameters.builder().encryptedRecord(encryptedRecord).encryptionConfig(encryptionConfig).build();
        DelegateTaskRequest delegateTaskRequest =
                DelegateTaskRequest.builder()
                        .taskType(FETCH_SECRET.name())
                        .taskParameters(parameters)
                        .executionTimeout(Duration.ofMillis(TaskData.DEFAULT_SYNC_CALL_TIMEOUT))
                        .accountId(accountId)
                        .build();
        DelegateResponseData delegateResponseData = delegateService.executeSyncTask(delegateTaskRequest);
        DelegateTaskUtils.validateDelegateTaskResponse(delegateResponseData);
        if (!(delegateResponseData instanceof FetchSecretTaskResponse)) {
            throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unknown Response from delegate", USER);
        }
        FetchSecretTaskResponse responseData = (FetchSecretTaskResponse) delegateResponseData;
        return responseData.getSecretValue();
    }

    public boolean validateReference(String accountId, ValidateSecretReferenceTaskParameters parameters) {
        DelegateTaskRequest delegateTaskRequest =
                DelegateTaskRequest.builder()
                        .taskType(VALIDATE_SECRET_REFERENCE.name())
                        .taskParameters(parameters)
                        .executionTimeout(Duration.ofMillis(TaskData.DEFAULT_SYNC_CALL_TIMEOUT))
                        .accountId(accountId)
                        .build();
        DelegateResponseData delegateResponseData = delegateService.executeSyncTask(delegateTaskRequest);
        DelegateTaskUtils.validateDelegateTaskResponse(delegateResponseData);
        if (!(delegateResponseData instanceof ValidateSecretReferenceTaskResponse)) {
            throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unknown Response from delegate", USER);
        }
        ValidateSecretReferenceTaskResponse responseData = (ValidateSecretReferenceTaskResponse) delegateResponseData;
        return responseData.isReferenceValid();
    }

    public boolean validateConfiguration(String accountId, ValidateSecretManagerConfigurationTaskParameters parameters) {
        DelegateTaskRequest delegateTaskRequest =
                DelegateTaskRequest.builder()
                        .taskType(VALIDATE_SECRET_MANAGER_CONFIGURATION.name())
                        .taskParameters(parameters)
                        .executionTimeout(Duration.ofMillis(TaskData.DEFAULT_SYNC_CALL_TIMEOUT))
                        .accountId(accountId)
                        .build();
        DelegateResponseData delegateResponseData = delegateService.executeSyncTask(delegateTaskRequest);
        DelegateTaskUtils.validateDelegateTaskResponse(delegateResponseData);
        if (!(delegateResponseData instanceof ValidateSecretManagerConfigurationTaskResponse)) {
            throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unknown Response from delegate", USER);
        }
        ValidateSecretManagerConfigurationTaskResponse responseData =
                (ValidateSecretManagerConfigurationTaskResponse) delegateResponseData;
        return responseData.isConfigurationValid();
    }
}