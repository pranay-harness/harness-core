package io.harness.connector.validator;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpDelegateDetailsDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.gcp.request.GcpValidationRequest;
import io.harness.delegate.task.gcp.request.GcpValidationRequest.GcpValidationRequestBuilder;
import io.harness.delegate.task.gcp.response.GcpValidationTaskResponse;
import io.harness.exception.InvalidRequestException;

public class GcpConnectorValidator extends AbstractConnectorValidator implements ConnectionValidator {
  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    GcpConnectorDTO gcpConnectorDTO = (GcpConnectorDTO) connectorConfig;
    GcpConnectorCredentialDTO gcpConnector = gcpConnectorDTO.getCredential();
    final GcpValidationRequestBuilder gcpValidationRequestBuilder = GcpValidationRequest.builder();
    switch (gcpConnector.getGcpCredentialType()) {
      case MANUAL_CREDENTIALS:
        final GcpManualDetailsDTO gcpDetailsDTO = (GcpManualDetailsDTO) gcpConnector.getConfig();
        return gcpValidationRequestBuilder.gcpManualDetailsDTO(gcpDetailsDTO)
            .encryptedDataDetailList(
                super.getEncryptionDetail(gcpDetailsDTO, accountIdentifier, orgIdentifier, projectIdentifier))
            .build();
      case INHERIT_FROM_DELEGATE:
        final GcpDelegateDetailsDTO config = (GcpDelegateDetailsDTO) gcpConnector.getConfig();
        return gcpValidationRequestBuilder.delegateSelector(config.getDelegateSelector()).build();
      default:
        throw new InvalidRequestException("Invalid credential type: " + gcpConnector.getGcpCredentialType());
    }
  }

  @Override
  public String getTaskType() {
    return "GCP_TASK";
  }

  @Override
  public ConnectorValidationResult validate(
      ConnectorConfigDTO connectorDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    final GcpValidationTaskResponse gcpValidationTaskResponse = (GcpValidationTaskResponse) super.validateConnector(
        connectorDTO, accountIdentifier, orgIdentifier, projectIdentifier);
    return gcpValidationTaskResponse.getConnectorValidationResult();
  }
}
