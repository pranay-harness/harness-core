package io.harness.connector.validator.scmValidators;

import static software.wings.beans.TaskType.NG_GIT_COMMAND;

import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.validator.AbstractConnectorValidator;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.delegate.beans.git.GitCommandType;
import io.harness.delegate.task.TaskParameters;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnknownEnumTypeException;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.Objects;

public abstract class AbstractGitConnector extends AbstractConnectorValidator {
  @Inject NGErrorHelper ngErrorHelper;

  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    GitConfigDTO gitConfig = (GitConfigDTO) connectorConfig;
    GitAuthenticationDTO gitAuthentication = gitConfig.getGitAuth();
    return GitCommandParams.builder()
        .gitConfig(gitConfig)
        .gitCommandType(GitCommandType.VALIDATE)
        .encryptionDetails(
            super.getEncryptionDetail(gitAuthentication, accountIdentifier, orgIdentifier, projectIdentifier))
        .build();
  }

  public abstract GitConfigDTO getGitConfigFromConnectorConfig(ConnectorConfigDTO connectorConfig);

  @Override
  public String getTaskType() {
    return NG_GIT_COMMAND.name();
  }

  public void validateFieldsPresent(GitConfigDTO gitConfig) {
    switch (gitConfig.getGitAuthType()) {
      case HTTP:
        GitHTTPAuthenticationDTO gitAuthenticationDTO = (GitHTTPAuthenticationDTO) gitConfig.getGitAuth();
        validateRequiredFieldsPresent(gitAuthenticationDTO.getPasswordRef(), gitConfig.getUrl(),
            gitAuthenticationDTO.getUsername(), gitConfig.getGitConnectionType());
        break;
      case SSH:
        throw new InvalidRequestException("Not implemented");
      default:
        throw new UnknownEnumTypeException("Git Authentication Type",
            gitConfig.getGitAuthType() == null ? null : gitConfig.getGitAuthType().getDisplayName());
    }
  }

  public ConnectorValidationResult buildConnectorValidationResult(
      GitCommandExecutionResponse gitCommandExecutionResponse) {
    String delegateId = null;
    if (gitCommandExecutionResponse.getDelegateMetaInfo() != null) {
      delegateId = gitCommandExecutionResponse.getDelegateMetaInfo().getId();
    }
    ConnectorValidationResult validationResult = gitCommandExecutionResponse.getConnectorValidationResult();
    if (validationResult != null) {
      validationResult.setDelegateId(delegateId);
    }
    return validationResult;
  }

  private void validateRequiredFieldsPresent(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required field is empty."));
  }

  public ConnectorValidationResult validate(
      ConnectorConfigDTO connectorConfigDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    final GitConfigDTO gitConfig = getGitConfigFromConnectorConfig(connectorConfigDTO);
    validateFieldsPresent(gitConfig);
    GitCommandExecutionResponse gitCommandExecutionResponse = (GitCommandExecutionResponse) super.validateConnector(
        gitConfig, accountIdentifier, orgIdentifier, projectIdentifier);
    return buildConnectorValidationResult(gitCommandExecutionResponse);
  }
}
