package io.harness.connector.validator.scmValidators;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.adapter.GitlabToGitMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;

public class GitlabConnectorValidator extends AbstractGitConnectorValidator {
  @Override
  public GitConfigDTO getGitConfigFromConnectorConfig(ConnectorConfigDTO connectorConfig) {
    return GitlabToGitMapper.mapToGitConfigDTO((GitlabConnectorDTO) connectorConfig);
  }

  @Override
  public ConnectorValidationResult validate(
      ConnectorConfigDTO connectorConfigDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return super.validate(connectorConfigDTO, accountIdentifier, orgIdentifier, projectIdentifier);
  }
}
