package io.harness.connector.validator.scmValidators;

import io.harness.connector.scmMappers.GitlabToGitMapper;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;

import com.google.inject.Inject;

public class GitlabConnectorValidator extends AbstractGitConnector {
  @Inject GitlabToGitMapper gitlabToGitMapper;

  @Override
  public GitConfigDTO getGitConfigFromConnectorConfig(ConnectorConfigDTO connectorConfig) {
    return gitlabToGitMapper.mapToGitConfigDTO((GitlabConnectorDTO) connectorConfig);
  }

  @Override
  public ConnectorValidationResult validate(
      ConnectorConfigDTO connectorConfigDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return super.validate(connectorConfigDTO, accountIdentifier, orgIdentifier, projectIdentifier);
  }
}
