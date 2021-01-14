package io.harness.connector.validator;

import io.harness.beans.DecryptableEntity;
import io.harness.beans.DelegateTaskRequest;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractConnectorValidator implements ConnectionValidator {
  @Inject private SecretManagerClientService ngSecretService;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  public <T extends ConnectorConfigDTO> DelegateResponseData validateConnector(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    DelegateTaskRequest delegateTaskRequest =
        DelegateTaskRequest.builder()
            .accountId(accountIdentifier)
            .taskType(getTaskType())
            .taskParameters(getTaskParameters(connectorConfig, accountIdentifier, orgIdentifier, projectIdentifier))
            .executionTimeout(Duration.ofMinutes(2))
            .build();

    DelegateResponseData responseData = delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
    if (responseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) responseData;
      log.info("Error in validation task for connector : [{}] with failure types [{}]",
          errorNotifyResponseData.getErrorMessage(), errorNotifyResponseData.getFailureTypes());
      throw new InvalidRequestException(errorNotifyResponseData.getErrorMessage());
    } else if (responseData instanceof RemoteMethodReturnValueData
        && (((RemoteMethodReturnValueData) responseData).getException() instanceof InvalidRequestException)) {
      String errorMessage =
          ((InvalidRequestException) ((RemoteMethodReturnValueData) responseData).getException()).getMessage();
      throw new InvalidRequestException(errorMessage);
    }
    return responseData;
  }

  public List<EncryptedDataDetail> getEncryptionDetail(
      DecryptableEntity decryptableEntity, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (decryptableEntity == null) {
      return null;
    }
    NGAccess basicNGAccessObject = BaseNGAccess.builder()
                                       .accountIdentifier(accountIdentifier)
                                       .orgIdentifier(orgIdentifier)
                                       .projectIdentifier(projectIdentifier)
                                       .build();

    return ngSecretService.getEncryptionDetails(basicNGAccessObject, decryptableEntity);
  }

  public abstract <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  public abstract String getTaskType();
}