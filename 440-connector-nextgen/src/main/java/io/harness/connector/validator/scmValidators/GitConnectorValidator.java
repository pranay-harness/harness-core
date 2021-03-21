package io.harness.connector.validator.scmValidators;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(DX)
public class GitConnectorValidator extends AbstractGitConnectorValidator {
  @Override
  public GitConfigDTO getGitConfigFromConnectorConfig(ConnectorConfigDTO connectorConfig) {
    return (GitConfigDTO) connectorConfig;
  }

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorConfigDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    return super.validate(connectorConfigDTO, accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }
}
