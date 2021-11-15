package io.harness.connector.validator.scmValidators;

import static io.harness.connector.helper.GitApiAccessDecryptionHelper.hasApiAccess;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.TaskType.NG_GIT_COMMAND;

import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.validator.AbstractConnectorValidator;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthentication;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthentication;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractGitConnectorValidator extends AbstractConnectorValidator {
  @Inject GitConfigAuthenticationInfoHelper gitConfigAuthenticationInfoHelper;

  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    final GitConfig gitConfig = getGitConfigFromConnectorConfig(connectorConfig);
    SSHKeySpecDTO sshKeySpecDTO =
        gitConfigAuthenticationInfoHelper.getSSHKey(gitConfig, accountIdentifier, orgIdentifier, projectIdentifier);
    NGAccess ngAccess = getNgAccess(accountIdentifier, orgIdentifier, projectIdentifier);
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();

    List<EncryptedDataDetail> authenticationEncryptedDataDetails =
        gitConfigAuthenticationInfoHelper.getEncryptedDataDetails(gitConfig, sshKeySpecDTO, ngAccess);
    if (isNotEmpty(authenticationEncryptedDataDetails)) {
      encryptedDataDetails.addAll(authenticationEncryptedDataDetails);
    }
    ScmConnector scmConnector = (ScmConnector) connectorConfig;

    if (hasApiAccess(scmConnector)) {
      List<EncryptedDataDetail> apiAccessEncryptedDataDetail =
          gitConfigAuthenticationInfoHelper.getApiAccessEncryptedDataDetail(scmConnector, ngAccess);
      if (isNotEmpty(apiAccessEncryptedDataDetail)) {
        encryptedDataDetails.addAll(apiAccessEncryptedDataDetail);
      }
    }

    return GitCommandParams.builder()
        .gitConfig(gitConfig)
        .scmConnector(scmConnector)
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

  public abstract GitConfig getGitConfigFromConnectorConfig(ConnectorConfigDTO connectorConfig);

  @Override
  public String getTaskType() {
    return NG_GIT_COMMAND.name();
  }

  public void validateFieldsPresent(GitConfig gitConfig) {
    switch (gitConfig.getGitAuthType()) {
      case HTTP:
        GitHTTPAuthentication gitAuthenticationDTO = (GitHTTPAuthentication) gitConfig.getGitAuth();
        validateRequiredFieldsPresent(
            gitAuthenticationDTO.getPasswordRef(), gitConfig.getUrl(), gitConfig.getGitConnectionType());
        break;
      case SSH:
        GitSSHAuthentication gitSSHAuthentication = (GitSSHAuthentication) gitConfig.getGitAuth();
        validateRequiredFieldsPresent(gitSSHAuthentication.getEncryptedSshKey());
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
    final GitConfig gitConfig = getGitConfigFromConnectorConfig(connectorConfigDTO);
    validateFieldsPresent(gitConfig);
    GitCommandExecutionResponse gitCommandExecutionResponse = (GitCommandExecutionResponse) super.validateConnector(
        connectorConfigDTO, accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return buildConnectorValidationResult(gitCommandExecutionResponse);
  }
}
