package io.harness.connector.validator;

import io.harness.delegate.beans.connector.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.task.TaskParameters;

public class GitlabConnectorValidator extends AbstractConnectorValidator {
  @Override
  public ConnectorValidationResult validate(
      ConnectorConfigDTO connectorDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    // Setting always true until we implement.
    return ConnectorValidationResult.builder()
        .status(ConnectivityStatus.SUCCESS)
        .testedAt(System.currentTimeMillis())
        .build();
  }

  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return null;
  }

  @Override
  public String getTaskType() {
    return null;
  }
}
