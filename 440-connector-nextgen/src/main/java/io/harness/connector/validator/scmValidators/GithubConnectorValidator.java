package io.harness.connector.validator.scmValidators;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.adapter.GithubToGitMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.connector.scm.github.GithubConnector;

public class GithubConnectorValidator extends AbstractGitConnectorValidator {
  @Override
  public GitConfig getGitConfigFromConnectorConfig(ConnectorConfigDTO connectorConfig) {
    return GithubToGitMapper.mapToGitConfigDTO((GithubConnector) connectorConfig);
  }

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorConfigDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    return super.validate(connectorConfigDTO, accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }
}
