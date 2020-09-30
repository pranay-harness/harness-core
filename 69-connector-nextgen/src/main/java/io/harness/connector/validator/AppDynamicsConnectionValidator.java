package io.harness.connector.validator;

import static java.time.Duration.ofMinutes;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTaskRequest;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectionTaskParams;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectionTaskResponse;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class AppDynamicsConnectionValidator implements ConnectionValidator<AppDynamicsConnectorDTO> {
  private final SecretManagerClientService ngSecretService;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Override
  public ConnectorValidationResult validate(AppDynamicsConnectorDTO appDynamicsConnectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier) {
    NGAccess basicNGAccessObject = BaseNGAccess.builder()
                                       .accountIdentifier(accountIdentifier)
                                       .orgIdentifier(orgIdentifier)
                                       .projectIdentifier(projectIdentifier)
                                       .build();

    List<EncryptedDataDetail> encryptedDataDetailList =
        ngSecretService.getEncryptionDetails(basicNGAccessObject, appDynamicsConnectorDTO);

    final DelegateTaskRequest delegateTaskRequest =
        DelegateTaskRequest.builder()
            .accountId(accountIdentifier)
            .taskType("APPDYNAMICS_NG_CONFIGURATION_VALIDATE_TASK")
            .taskParameters(AppDynamicsConnectionTaskParams.builder()
                                .appDynamicsConnectorDTO(appDynamicsConnectorDTO)
                                .encryptionDetails(encryptedDataDetailList)
                                .build())
            .executionTimeout(ofMinutes(1))
            .taskSetupAbstraction("orgIdentifier", orgIdentifier)
            .taskSetupAbstraction("projectIdentifier", projectIdentifier)
            .build();
    DelegateResponseData delegateResponseData = delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
    if (delegateResponseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) delegateResponseData;
      return ConnectorValidationResult.builder()
          .valid(false)
          .errorMessage(errorNotifyResponseData.getErrorMessage())
          .build();
    }

    AppDynamicsConnectionTaskResponse appDynamicsConnectionTaskResponse =
        (AppDynamicsConnectionTaskResponse) delegateResponseData;
    return ConnectorValidationResult.builder()
        .valid(appDynamicsConnectionTaskResponse.isValid())
        .errorMessage(appDynamicsConnectionTaskResponse.getErrorMessage())
        .build();
  }
}
