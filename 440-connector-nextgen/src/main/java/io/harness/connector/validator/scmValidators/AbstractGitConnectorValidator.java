package io.harness.connector.validator.scmValidators;

import static io.harness.delegate.beans.connector.scm.GitAuthType.SSH;
import static io.harness.delegate.beans.connector.scm.GitConnectionType.ACCOUNT;

import static software.wings.beans.TaskType.NG_GIT_COMMAND;

import io.harness.beans.IdentifierRef;
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
import io.harness.encryption.SecretRefData;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.api.SecretCrudService;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.secretmanagerclient.services.SshKeySpecDTOHelper;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.utils.IdentifierRefHelper;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class AbstractGitConnectorValidator extends AbstractConnectorValidator {
  @Inject NGErrorHelper ngErrorHelper;
  @Inject SecretCrudService secretCrudService;
  @Inject SshKeySpecDTOHelper sshKeySpecDTOHelper;

  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    GitConfigDTO gitConfig = (GitConfigDTO) connectorConfig;
    SSHKeySpecDTO sshKeySpecDTO = getSSHKey(gitConfig, accountIdentifier, orgIdentifier, projectIdentifier);
    NGAccess ngAccess = getNgAccess(accountIdentifier, orgIdentifier, projectIdentifier);
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails(gitConfig, sshKeySpecDTO, ngAccess);
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

  private List<EncryptedDataDetail> getEncryptedDataDetails(
      GitConfigDTO gitConfig, SSHKeySpecDTO sshKeySpecDTO, NGAccess ngAccess) {
    switch (gitConfig.getGitAuthType()) {
      case HTTP:
        return super.getEncryptionDetail(gitConfig.getGitAuth(), ngAccess.getAccountIdentifier(),
            ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
      case SSH:
        return sshKeySpecDTOHelper.getSSHKeyEncryptionDetails(sshKeySpecDTO, ngAccess);
      default:
        throw new UnknownEnumTypeException("Git Authentication Type",
            gitConfig.getGitAuthType() == null ? null : gitConfig.getGitAuthType().getDisplayName());
    }
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

  private SSHKeySpecDTO getSSHKey(
      GitConfigDTO gitConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (gitConfig.getGitAuthType() != SSH) {
      return null;
    }
    GitSSHAuthenticationDTO gitAuthenticationDTO = (GitSSHAuthenticationDTO) gitConfig.getGitAuth();
    SecretRefData sshKeyRef = gitAuthenticationDTO.getEncryptedSshKey();
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
        sshKeyRef.getIdentifier(), accountIdentifier, orgIdentifier, projectIdentifier);
    Optional<SecretResponseWrapper> secretResponseWrapper = secretCrudService.get(identifierRef.getAccountIdentifier(),
        identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
    if (!secretResponseWrapper.isPresent()) {
      throw new InvalidRequestException("No secret configured with identifier: " + sshKeyRef);
    }
    SecretDTOV2 secret = secretResponseWrapper.get().getSecret();
    return (SSHKeySpecDTO) secret.getSpec();
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
    // No validation for account level git connector.
    if (gitConfig.getGitConnectionType() == ACCOUNT) {
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.SUCCESS)
          .testedAt(System.currentTimeMillis())
          .build();
    }
    validateFieldsPresent(gitConfig);
    GitCommandExecutionResponse gitCommandExecutionResponse = (GitCommandExecutionResponse) super.validateConnector(
        gitConfig, accountIdentifier, orgIdentifier, projectIdentifier);
    return buildConnectorValidationResult(gitCommandExecutionResponse);
  }
}
