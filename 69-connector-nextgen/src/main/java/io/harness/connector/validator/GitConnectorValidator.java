package io.harness.connector.validator;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTaskRequest;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.gitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.delegate.beans.git.GitCommandType;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class GitConnectorValidator implements ConnectionValidator<GitConfigDTO> {
  private DelegateGrpcClientWrapper delegateClient;
  private SecretManagerClientService ngSecretService;

  public ConnectorValidationResult validate(
      GitConfigDTO gitConfig, String accountIdentifier, String orgIdentifier, String projectIdentifie) {
    validateFieldsPresent(gitConfig);
    GitCommandExecutionResponse gitCommandExecutionResponse =
        createValidationDelegateTask(gitConfig, accountIdentifier, orgIdentifier, projectIdentifie);
    return buildConnectorValidationResult(gitCommandExecutionResponse);
  }

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
    if (gitCommandExecutionResponse != null
        && GitCommandStatus.SUCCESS == gitCommandExecutionResponse.getGitCommandStatus()) {
      return ConnectorValidationResult.builder().valid(true).build();
    } else {
      return ConnectorValidationResult.builder()
          .valid(false)
          .errorMessage(Optional.ofNullable(gitCommandExecutionResponse)
                            .map(GitCommandExecutionResponse::getErrorMessage)
                            .orElse("Error in making connection."))
          .build();
    }
  }

  private GitCommandExecutionResponse createValidationDelegateTask(
      GitConfigDTO gitConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    GitAuthenticationDTO gitAuthenticationDecryptableEntity = gitConfig.getGitAuth();
    NGAccess basicNGAccessObject = BaseNGAccess.builder()
                                       .accountIdentifier(accountIdentifier)
                                       .orgIdentifier(orgIdentifier)
                                       .projectIdentifier(projectIdentifier)
                                       .build();
    List<EncryptedDataDetail> encryptedDataDetailList =
        ngSecretService.getEncryptionDetails(basicNGAccessObject, gitAuthenticationDecryptableEntity);
    DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                  .accountId(accountIdentifier)
                                                  .taskType("NG_GIT_COMMAND")
                                                  .taskParameters(GitCommandParams.builder()
                                                                      .gitConfig(gitConfig)
                                                                      .gitCommandType(GitCommandType.VALIDATE)
                                                                      .encryptionDetails(encryptedDataDetailList)
                                                                      .build())
                                                  .executionTimeout(Duration.ofMinutes(1))
                                                  .build();
    return (GitCommandExecutionResponse) delegateClient.executeSyncTask(delegateTaskRequest);
  }

  private void validateRequiredFieldsPresent(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }
}
