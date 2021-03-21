package io.harness.connector.validator;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static software.wings.beans.TaskType.GCP_TASK;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.gcp.request.GcpRequest.RequestType;
import io.harness.delegate.task.gcp.request.GcpValidationRequest;
import io.harness.delegate.task.gcp.request.GcpValidationRequest.GcpValidationRequestBuilder;
import io.harness.delegate.task.gcp.response.GcpValidationTaskResponse;
import io.harness.exception.InvalidRequestException;
@OwnedBy(DX)
public class GcpConnectorValidator extends AbstractConnectorValidator {
  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    GcpConnectorDTO gcpConnectorDTO = (GcpConnectorDTO) connectorConfig;
    GcpConnectorCredentialDTO gcpConnector = gcpConnectorDTO.getCredential();
    final GcpValidationRequestBuilder<?, ?> gcpValidationRequestBuilder = GcpValidationRequest.builder();
    switch (gcpConnector.getGcpCredentialType()) {
      case MANUAL_CREDENTIALS:
        final GcpManualDetailsDTO gcpDetailsDTO = (GcpManualDetailsDTO) gcpConnector.getConfig();
        return gcpValidationRequestBuilder.gcpManualDetailsDTO(gcpDetailsDTO)
            .requestType(RequestType.VALIDATE)
            .encryptionDetails(
                super.getEncryptionDetail(gcpDetailsDTO, accountIdentifier, orgIdentifier, projectIdentifier))
            .build();
      case INHERIT_FROM_DELEGATE:
        return gcpValidationRequestBuilder.requestType(RequestType.VALIDATE)
            .delegateSelectors(gcpConnectorDTO.getDelegateSelectors())
            .build();
      default:
        throw new InvalidRequestException("Invalid credential type: " + gcpConnector.getGcpCredentialType());
    }
  }
  @Override
  public String getTaskType() {
    return GCP_TASK.name();
  }
  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    final GcpValidationTaskResponse gcpValidationTaskResponse = (GcpValidationTaskResponse) super.validateConnector(
        connectorDTO, accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return gcpValidationTaskResponse.getConnectorValidationResult();
  }
}