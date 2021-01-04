package io.harness.connector.validator;

import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;

public class GithubConnectorValidator implements ConnectionValidator<GithubConnectorDTO> {
  @Override
  public ConnectorValidationResult validate(
      GithubConnectorDTO connectorDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    // Setting always true until we implement.
    return ConnectorValidationResult.builder().valid(true).testedAt(System.currentTimeMillis()).build();
  }
}
