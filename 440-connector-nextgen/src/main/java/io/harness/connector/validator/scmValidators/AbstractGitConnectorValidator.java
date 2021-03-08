package io.harness.connector.validator.scmValidators;

import static io.harness.delegate.beans.connector.scm.GitConnectionType.ACCOUNT;

import static software.wings.beans.TaskType.NG_GIT_COMMAND;

import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.validator.AbstractConnectorValidator;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthenticationDTO;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.delegate.beans.git.GitCommandType;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.List;
import java.util.Objects;

public abstract class AbstractGitConnectorValidator extends AbstractConnectorValidator {
  @Inject GitConfigAuthenticationInfoHelper gitConfigAuthenticationInfoHelper;

  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    GitConfigDTO gitConfig = (GitConfigDTO) connectorConfig;
    SSHKeySpecDTO sshKeySpecDTO =
        gitConfigAuthenticationInfoHelper.getSSHKey(gitConfig, accountIdentifier, orgIdentifier, projectIdentifier);
    NGAccess ngAccess = getNgAccess(accountIdentifier, orgIdentifier, projectIdentifier);
    List<EncryptedDataDetail> encryptedDataDetails =
        gitConfigAuthenticationInfoHelper.getEncryptedDataDetails(gitConfig, sshKeySpecDTO, ngAccess);
    return GitCommandParams.builder()
        .gitConfig(gitConfig)
        .sshKeySpecDTO(sshKeySpecDTO)
        .gitCommandType(GitCommandType.VALIDATE)
        .encryptionDetails(encryptedDataDetails)
        .build();
  }

  private NGAccess getNgAccess(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return BaseNGAccess.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
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
        validateRequiredFieldsPresent(
            gitAuthenticationDTO.getPasswordRef(), gitConfig.getUrl(), gitConfig.getGitConnectionType());
        break;
      case SSH:
        GitSSHAuthenticationDTO gitSSHAuthenticationDTO = (GitSSHAuthenticationDTO) gitConfig.getGitAuth();
        validateRequiredFieldsPresent(gitSSHAuthenticationDTO.getEncryptedSshKey());
        break;
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

  public ConnectorValidationResult validate(ConnectorConfigDTO connectorConfigDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    final GitConfigDTO gitConfig = getGitConfigFromConnectorConfig(connectorConfigDTO);
    // No validation for account level git connector.
    if (gitConfig.getGitConnectionType() == ACCOUNT) {
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.SUCCESS)
          .testedAt(System.currentTimeMillis())
          .build();
    }
    validateFieldsPresent(gitConfig);
    GitCommandExecutionResponse gitCommandExecutionResponse = (GitCommandExecutionResponse) super.validateConnector(
        gitConfig, accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return buildConnectorValidationResult(gitCommandExecutionResponse);
  }
}
