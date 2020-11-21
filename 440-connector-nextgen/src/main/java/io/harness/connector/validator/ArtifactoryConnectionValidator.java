package io.harness.connector.validator;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.task.TaskParameters;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ArtifactoryConnectionValidator
    extends AbstractConnectorValidator implements ConnectionValidator<ArtifactoryConnectorDTO> {
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
      ArtifactoryConnectorDTO connectorDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return ConnectorValidationResult.builder().valid(true).build();
  }
}
