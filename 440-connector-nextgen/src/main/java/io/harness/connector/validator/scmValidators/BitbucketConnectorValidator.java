package io.harness.connector.validator.scmValidators;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.adapter.BitbucketToGitMapper;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;

@OwnedBy(DX)
public class BitbucketConnectorValidator extends AbstractGitConnectorValidator {
  @Override
  public GitConfigDTO getGitConfigFromConnectorConfig(ConnectorConfigDTO connectorConfig) {
    return BitbucketToGitMapper.mapToGitConfigDTO((BitbucketConnectorDTO) connectorConfig);
  }

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorConfigDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    return super.validate(connectorConfigDTO, accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }
}
