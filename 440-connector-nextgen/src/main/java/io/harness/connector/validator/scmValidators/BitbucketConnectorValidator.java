package io.harness.connector.validator.scmValidators;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.adapter.BitbucketToGitMapper;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnector;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;

public class BitbucketConnectorValidator extends AbstractGitConnectorValidator {
  @Override
  public GitConfig getGitConfigFromConnectorConfig(ConnectorConfigDTO connectorConfig) {
    return BitbucketToGitMapper.mapToGitConfigDTO((BitbucketConnector) connectorConfig);
  }

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorConfigDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    return super.validate(connectorConfigDTO, accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }
}
