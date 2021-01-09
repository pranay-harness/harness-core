package io.harness.connector.validator;

import io.harness.delegate.beans.connector.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;

public class GitlabConnectorValidator implements ConnectionValidator<GitlabConnectorDTO> {
  @Override
  public ConnectorValidationResult validate(
      GitlabConnectorDTO connectorDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    // Setting always true until we implement.
    return ConnectorValidationResult.builder()
        .status(ConnectivityStatus.SUCCESS)
        .testedAt(System.currentTimeMillis())
        .build();
  }
}
