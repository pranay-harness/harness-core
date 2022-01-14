package io.harness.connector.validator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.servicenow.ServiceNowConnectionTaskParams;
import io.harness.delegate.beans.connector.servicenow.ServiceNowConnectorDTO;
import io.harness.delegate.beans.connector.servicenow.connection.ServiceNowTestConnectionTaskNGResponse;
import io.harness.delegate.task.TaskParameters;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Slf4j
@Singleton
public class ServiceNowConnectorValidator extends AbstractConnectorValidator {
  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    ServiceNowConnectorDTO serviceNowConnectorDTO = (ServiceNowConnectorDTO) connectorConfig;

    return ServiceNowConnectionTaskParams.builder()
        .serviceNowConnectorDTO(serviceNowConnectorDTO)
        .encryptionDetails(
            super.getEncryptionDetail(serviceNowConnectorDTO, accountIdentifier, orgIdentifier, projectIdentifier))
        .build();
  }

  @Override
  public String getTaskType() {
    return "SERVICENOW_CONNECTIVITY_TASK_NG";
  }

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO jiraConnectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    ServiceNowTestConnectionTaskNGResponse delegateResponseData =
        (ServiceNowTestConnectionTaskNGResponse) super.validateConnector(
            jiraConnectorDTO, accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return ConnectorValidationResult.builder()
        .delegateId(delegateResponseData.getDelegateMetaInfo() == null
                ? null
                : delegateResponseData.getDelegateMetaInfo().getId())
        .status(delegateResponseData.getCanConnect() ? ConnectivityStatus.SUCCESS : ConnectivityStatus.FAILURE)
        .errorSummary(delegateResponseData.getErrorMessage())
        .build();
  }

  @Override
  public ConnectorValidationResult validate(ConnectorResponseDTO connectorResponseDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    return null;
  }
}
