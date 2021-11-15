package io.harness.connector.validator.scmValidators;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.adapter.GitlabToGitMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnector;

public class GitlabConnectorValidator extends AbstractGitConnectorValidator {
  @Override
  public GitConfig getGitConfigFromConnectorConfig(ConnectorConfigDTO connectorConfig) {
    return GitlabToGitMapper.mapToGitConfigDTO((GitlabConnector) connectorConfig);
  }

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    return super.validate(connectorDTO, accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }
}
