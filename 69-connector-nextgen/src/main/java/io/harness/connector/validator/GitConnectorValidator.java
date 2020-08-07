package io.harness.connector.validator;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ManagerDelegateServiceDriver;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.gitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.git.GitCommand.GitCommandType;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class GitConnectorValidator implements ConnectionValidator<GitConfigDTO> {
  private ManagerDelegateServiceDriver managerDelegateServiceDriver;
  private SecretManagerClientService ngSecretService;

  public ConnectorValidationResult validate(
      GitConfigDTO gitConfig, String accountIdentifier, String orgIdentifier, String projectIdentifie) {
    validateFieldsPresent(gitConfig);
    GitCommandExecutionResponse gitCommandExecutionResponse =
        createValidationDelegateTask(gitConfig, accountIdentifier);
    return buildConnectorValidationResult(gitCommandExecutionResponse);
  }

  private void validateFieldsPresent(GitConfigDTO gitConfig) {
    switch (gitConfig.getGitAuthType()) {
      case HTTP:
        GitHTTPAuthenticationDTO gitAuthenticationDTO = (GitHTTPAuthenticationDTO) gitConfig.getGitAuth();
        validateRequiredFieldsPresent(gitAuthenticationDTO.getEncryptedPassword(), gitAuthenticationDTO.getUrl(),
            gitAuthenticationDTO.getUsername(), gitAuthenticationDTO.getGitConnectionType(),
            gitAuthenticationDTO.getBranchName());
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

  private GitCommandExecutionResponse createValidationDelegateTask(GitConfigDTO gitConfig, String accountId) {
    Map<String, String> setupAbstractions = ImmutableMap.of("accountId", accountId);
    GitAuthenticationDTO gitAuthenticationEncryptedSetting = gitConfig.getGitAuth();
    List<EncryptedDataDetail> encryptedDataDetailList =
        ngSecretService.getEncryptionDetails(gitAuthenticationEncryptedSetting);
    TaskData taskData = TaskData.builder()
                            .async(false)
                            .taskType("NG_GIT_COMMAND")
                            .parameters(new Object[] {GitCommandParams.builder()
                                                          .gitConfig(gitConfig)
                                                          .gitCommandType(GitCommandType.VALIDATE)
                                                          .encryptionDetails(encryptedDataDetailList)
                                                          .build()})
                            .timeout(TimeUnit.MINUTES.toMillis(1))
                            .build();
    return managerDelegateServiceDriver.sendTask(accountId, setupAbstractions, taskData);
  }

  private void validateRequiredFieldsPresent(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }
}
