package io.harness.connector.validator;

import io.harness.delegate.beans.connector.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorValidationResult.ConnectorValidationResultBuilder;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.delegate.beans.git.GitCommandType;
import io.harness.delegate.task.TaskParameters;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnknownEnumTypeException;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class GitConnectorValidator extends AbstractConnectorValidator {
  @Inject NGErrorHelper ngErrorHelper;

  private void validateFieldsPresent(GitConfigDTO gitConfig) {
    switch (gitConfig.getGitAuthType()) {
      case HTTP:
        GitHTTPAuthenticationDTO gitAuthenticationDTO = (GitHTTPAuthenticationDTO) gitConfig.getGitAuth();
        validateRequiredFieldsPresent(gitAuthenticationDTO.getPasswordRef(), gitConfig.getUrl(),
            gitAuthenticationDTO.getUsername(), gitConfig.getGitConnectionType(), gitConfig.getBranchName());
        break;
      case SSH:
        throw new InvalidRequestException("Not implemented");
      default:
        throw new UnknownEnumTypeException("Git Authentication Type",
            gitConfig.getGitAuthType() == null ? null : gitConfig.getGitAuthType().getDisplayName());
    }
  }

  private ConnectorValidationResult buildConnectorValidationResult(
      GitCommandExecutionResponse gitCommandExecutionResponse) {
    String delegateId = null;
    if (gitCommandExecutionResponse.getDelegateMetaInfo() != null) {
      delegateId = gitCommandExecutionResponse.getDelegateMetaInfo().getId();
    }
    ConnectorValidationResultBuilder validationResultBuilder =
        ConnectorValidationResult.builder().delegateId(delegateId).testedAt(System.currentTimeMillis());
    if (gitCommandExecutionResponse != null
        && GitCommandStatus.SUCCESS == gitCommandExecutionResponse.getGitCommandStatus()) {
      validationResultBuilder.status(ConnectivityStatus.SUCCESS);
    } else {
      String errorMessage = gitCommandExecutionResponse.getErrorMessage();
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .errorSummary(ngErrorHelper.getErrorSummary(errorMessage))
          .errors(Collections.singletonList(ngErrorHelper.createErrorDetail(errorMessage)))
          .build();
    }
    return validationResultBuilder.build();
  }

  private void validateRequiredFieldsPresent(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

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

  @Override
  public String getTaskType() {
    return "NG_GIT_COMMAND";
  }

  @Override
  public ConnectorValidationResult validate(
      ConnectorConfigDTO gitConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    validateFieldsPresent((GitConfigDTO) gitConfig);
    GitCommandExecutionResponse gitCommandExecutionResponse = (GitCommandExecutionResponse) super.validateConnector(
        gitConfig, accountIdentifier, orgIdentifier, projectIdentifier);
    return buildConnectorValidationResult(gitCommandExecutionResponse);
  }
}
