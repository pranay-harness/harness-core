package io.harness.connector.validator;

import io.harness.delegate.beans.connector.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.task.TaskParameters;

import com.google.inject.Singleton;

@Singleton
public class NexusConnectorValidator extends AbstractConnectorValidator {
  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return null;
  }

  @Override
  public String getTaskType() {
    return null;
  }

  @Override
  public ConnectorValidationResult validate(
      ConnectorConfigDTO connectorDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    // making always true.
    return ConnectorValidationResult.builder().status(ConnectivityStatus.SUCCESS).build();
  }
}
