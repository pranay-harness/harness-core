package io.harness.connector.validator.scmValidators;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class GitConnectorValidator extends AbstractGitConnectorValidator {
  @Override
  public GitConfig getGitConfigFromConnectorConfig(ConnectorConfigDTO connectorConfig) {
    return (GitConfig) connectorConfig;
  }

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorConfigDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    return super.validate(connectorConfigDTO, accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }
}
