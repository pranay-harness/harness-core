package io.harness.connector.validator;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.TaskType.CVNG_CONNECTOR_VALIDATE_TASK;

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
import io.harness.delegate.task.TaskParameters;
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
public class CVConnectorValidator extends AbstractConnectorValidator {
  @Inject private final SecretManagerClientService ngSecretService;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Inject private NGErrorHelper ngErrorHelper;

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

  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    final List<EncryptedDataDetail> encryptionDetail =
        super.getEncryptionDetail(connectorConfig, accountIdentifier, orgIdentifier, projectIdentifier);
    return CVConnectorTaskParams.builder()
        .connectorConfigDTO(connectorConfig)
        .encryptionDetails(encryptionDetail)
        .build();
  }

  @Override
  public String getTaskType() {
    return CVNG_CONNECTOR_VALIDATE_TASK.name();
  }

  @Override
  public ConnectorValidationResult validate(
      ConnectorConfigDTO connectorDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    final DelegateResponseData delegateResponseData =
        super.validateConnector(connectorDTO, accountIdentifier, orgIdentifier, projectIdentifier);
    CVConnectorTaskResponse cvConnectorTaskResponse = (CVConnectorTaskResponse) delegateResponseData;
    return ConnectorValidationResult.builder()
        .status(cvConnectorTaskResponse.isValid() ? ConnectivityStatus.SUCCESS : ConnectivityStatus.FAILURE)
        .errorSummary(cvConnectorTaskResponse.getErrorMessage())
        .delegateId(getDelegateId(cvConnectorTaskResponse))
        .build();
  }
}
