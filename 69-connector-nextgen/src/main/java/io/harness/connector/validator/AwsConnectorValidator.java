package io.harness.connector.validator;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.task.TaskParameters;

// todo(abhinav): create delegate task post client creation
public class AwsConnectorValidator extends AbstractConnectorValidator implements ConnectionValidator<AwsConnectorDTO> {
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
      AwsConnectorDTO connectorDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return null;
  }
}
