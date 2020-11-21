package io.harness.connector.validator;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.docker.DockerAuthCredentialsDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerTestConnectionTaskParams;
import io.harness.delegate.beans.connector.docker.DockerTestConnectionTaskResponse;
import io.harness.delegate.task.TaskParameters;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DockerConnectionValidator
    extends AbstractConnectorValidator implements ConnectionValidator<DockerConnectorDTO> {
  @Override
  public ConnectorValidationResult validate(
      DockerConnectorDTO dockerConnector, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    DockerTestConnectionTaskResponse responseData = (DockerTestConnectionTaskResponse) super.validateConnector(
        dockerConnector, accountIdentifier, orgIdentifier, projectIdentifier);
    return ConnectorValidationResult.builder()
        .valid(responseData.getConnectionSuccessFul())
        .errorMessage(responseData.getErrorMessage())
        .build();
  }

  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    DockerConnectorDTO dockerConnector = (DockerConnectorDTO) connectorConfig;
    DockerAuthCredentialsDTO dockerAuthCredentials =
        dockerConnector.getAuth() != null ? dockerConnector.getAuth().getCredentials() : null;
    return DockerTestConnectionTaskParams.builder()
        .dockerConnector(dockerConnector)
        .encryptionDetails(
            super.getEncryptionDetail(dockerAuthCredentials, accountIdentifier, orgIdentifier, projectIdentifier))
        .build();
  }

  @Override
  public String getTaskType() {
    return "DOCKER_CONNECTIVITY_TEST_TASK";
  }
}
