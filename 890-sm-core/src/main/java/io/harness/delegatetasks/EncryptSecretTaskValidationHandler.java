package io.harness.delegatetasks;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;

import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsConnectorDTO;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsValidationParams;
import io.harness.delegate.task.ConnectorValidationHandler;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.mappers.SecretManagerConfigMapper;
import io.harness.ng.SecretManagerConfigDTOMapper;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;

public class EncryptSecretTaskValidationHandler implements ConnectorValidationHandler {
  @Inject KmsEncryptorsRegistry kmsEncryptorsRegistry;
  @Inject NGErrorHelper ngErrorHelper;

  @Override
  public ConnectorValidationResult validate(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    long currentTime = System.currentTimeMillis();
    EncryptSecretTaskResponse encryptSecretTaskResponse;
    try {
      EncryptSecretTaskParameters encryptSecretTaskParameters =
          getTaskParams(connectorValidationParams, accountIdentifier);
      encryptSecretTaskResponse = EncryptSecretTask.run(encryptSecretTaskParameters, kmsEncryptorsRegistry);
    } catch (Exception exception) {
      String errorMessage = exception.getMessage();
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .testedAt(currentTime)
          .errorSummary(ngErrorHelper.createErrorSummary("Invalid Credentials", errorMessage))
          .errors(getErrorDetail(errorMessage))
          .build();
    }

    if (encryptSecretTaskResponse.getEncryptedRecord() != null) {
      return ConnectorValidationResult.builder().status(ConnectivityStatus.SUCCESS).testedAt(currentTime).build();
    } else {
      return ConnectorValidationResult.builder().status(ConnectivityStatus.FAILURE).testedAt(currentTime).build();
    }
  }

  private EncryptSecretTaskParameters getTaskParams(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    if (ConnectorType.GCP_KMS.equals(connectorValidationParams.getConnectorType())) {
      GcpKmsConnectorDTO gcpKmsConnectorDTO =
          ((GcpKmsValidationParams) connectorValidationParams).getGcpKmsConnectorDTO();
      ConnectorInfoDTO connectorInfoDTO =
          ConnectorInfoDTO.builder().connectorConfig(gcpKmsConnectorDTO).connectorType(ConnectorType.VAULT).build();
      SecretManagerConfigDTO secretManagerConfigDTO = SecretManagerConfigDTOMapper.fromConnectorDTO(
          accountIdentifier, ConnectorDTO.builder().connectorInfo(connectorInfoDTO).build(), gcpKmsConnectorDTO);
      return EncryptSecretTaskParameters.builder()
          .encryptionConfig(SecretManagerConfigMapper.fromDTO(secretManagerConfigDTO))
          .value(UUIDGenerator.generateUuid())
          .build();
    }
    throw new SecretManagementDelegateException(
        SECRET_MANAGEMENT_ERROR, "Secret Manager not supported for encrypt secret task type.", USER);
  }

  private List<ErrorDetail> getErrorDetail(String errorMessage) {
    return Collections.singletonList(
        ErrorDetail.builder().message(errorMessage).code(450).reason("Invalid Credentials").build());
  }
}
