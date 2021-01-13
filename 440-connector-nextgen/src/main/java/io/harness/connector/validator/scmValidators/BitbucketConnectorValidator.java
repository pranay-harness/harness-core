package io.harness.connector.validator.scmValidators;

import io.harness.connector.scmMappers.BitbucketToGitMapper;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;

import com.google.inject.Inject;

public class BitbucketConnectorValidator extends AbstractGitConnector {
  @Inject BitbucketToGitMapper bitbucketToGitMapper;

  @Override
  public GitConfigDTO getGitConfigFromConnectorConfig(ConnectorConfigDTO connectorConfig) {
    return bitbucketToGitMapper.mapToGitConfigDTO((BitbucketConnectorDTO) connectorConfig);
  }

  @Override
  public ConnectorValidationResult validate(
      ConnectorConfigDTO connectorConfigDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return super.validate(connectorConfigDTO, accountIdentifier, orgIdentifier, projectIdentifier);
  }
}
