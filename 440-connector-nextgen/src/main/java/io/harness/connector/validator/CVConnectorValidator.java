package io.harness.connector.validator;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.time.Duration.ofMinutes;

import io.harness.beans.DecryptableEntity;
import io.harness.beans.DelegateTaskRequest;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.connector.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorValidationResult.ConnectorValidationResultBuilder;
import io.harness.delegate.beans.connector.cvconnector.CVConnectorTaskParams;
import io.harness.delegate.beans.connector.cvconnector.CVConnectorTaskResponse;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class CVConnectorValidator<T extends ConnectorConfigDTO> implements ConnectionValidator<T> {
  @Inject private final SecretManagerClientService ngSecretService;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Inject private NGErrorHelper ngErrorHelper;

  @Override
  public ConnectorValidationResult validate(
      T connectorDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    NGAccess basicNGAccessObject = BaseNGAccess.builder()
                                       .accountIdentifier(accountIdentifier)
                                       .orgIdentifier(orgIdentifier)
                                       .projectIdentifier(projectIdentifier)
                                       .build();

    List<EncryptedDataDetail> encryptedDataDetailList = new ArrayList<>();
    if (connectorDTO instanceof DecryptableEntity) {
      encryptedDataDetailList =
          ngSecretService.getEncryptionDetails(basicNGAccessObject, (DecryptableEntity) connectorDTO);
    }

    final DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                        .accountId(accountIdentifier)
                                                        .taskType("CVNG_CONNECTOR_VALIDATE_TASK")
                                                        .taskParameters(CVConnectorTaskParams.builder()
                                                                            .connectorConfigDTO(connectorDTO)
                                                                            .encryptionDetails(encryptedDataDetailList)
                                                                            .build())
                                                        .executionTimeout(ofMinutes(1))
                                                        .taskSetupAbstraction("orgIdentifier", orgIdentifier)
                                                        .taskSetupAbstraction("projectIdentifier", projectIdentifier)
                                                        .build();
    DelegateResponseData delegateResponseData = delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
    if (delegateResponseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) delegateResponseData;
      String errorMessage = errorNotifyResponseData.getErrorMessage();
      String errorSummary = ngErrorHelper.createErrorSummary("Invalid Credentials", errorMessage);
      List<ErrorDetail> errorDetail = getErrorDetail(errorMessage);
      ConnectorValidationResultBuilder connectorValidationResult =
          ConnectorValidationResult.builder()
              .status(ConnectivityStatus.FAILURE)
              .testedAt(System.currentTimeMillis())
              .delegateId(getDelegateId((ErrorNotifyResponseData) delegateResponseData));
      if (isNotEmpty(errorMessage)) {
        connectorValidationResult.errorSummary(errorSummary).errors(errorDetail);
      }
      return connectorValidationResult.build();
    } else if (delegateResponseData instanceof RemoteMethodReturnValueData) {
      return ConnectorValidationResult.builder().status(ConnectivityStatus.FAILURE).build();
    }

    CVConnectorTaskResponse cvConnectorTaskResponse = (CVConnectorTaskResponse) delegateResponseData;
    return ConnectorValidationResult.builder()
        .status(cvConnectorTaskResponse.isValid() ? ConnectivityStatus.SUCCESS : ConnectivityStatus.FAILURE)
        .delegateId(getDelegateId(cvConnectorTaskResponse))
        .build();
  }

  private String getDelegateId(DelegateTaskNotifyResponseData cvConnectorTaskResponse) {
    if (cvConnectorTaskResponse == null || cvConnectorTaskResponse.getDelegateMetaInfo() == null) {
      return null;
    }
    return cvConnectorTaskResponse.getDelegateMetaInfo().getId();
  }

  private List<ErrorDetail> getErrorDetail(String errorMessage) {
    return Collections.singletonList(
        ErrorDetail.builder().message(errorMessage).code(450).reason("Invalid Credentials").build());
  }
}
