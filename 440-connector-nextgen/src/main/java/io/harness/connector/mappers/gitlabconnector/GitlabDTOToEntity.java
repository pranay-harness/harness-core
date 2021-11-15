package io.harness.connector.mappers.gitlabconnector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabHttpAuth;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabHttpAuthentication;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabSshAuthentication;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabTokenApiAccess;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccess;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessSpec;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabAuthentication;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnector;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabCredentials;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpCredentials;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabKerberos;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabSshCredentials;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpec;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernamePassword;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernameToken;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.UnknownEnumTypeException;

@OwnedBy(HarnessTeam.DX)
public class GitlabDTOToEntity implements ConnectorDTOToEntityMapper<GitlabConnector,
    io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector> {
  @Override
  public io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector toConnectorEntity(
      GitlabConnector configDTO) {
    GitAuthType gitAuthType = getAuthType(configDTO.getAuthentication());
    io.harness.connector.entities.embedded.gitlabconnector.GitlabAuthentication gitlabAuthentication =
        buildAuthenticationDetails(gitAuthType, configDTO.getAuthentication().getCredentials());
    boolean hasApiAccess = hasApiAccess(configDTO.getApiAccess());
    GitlabApiAccessType apiAccessType = null;
    GitlabTokenApiAccess gitlabApiAccess = null;
    if (hasApiAccess) {
      apiAccessType = getApiAccessType(configDTO.getApiAccess());
      gitlabApiAccess = getApiAcessByType(configDTO.getApiAccess().getSpec(), apiAccessType);
    }
    return io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector.builder()
        .connectionType(configDTO.getConnectionType())
        .authType(gitAuthType)
        .hasApiAccess(hasApiAccess)
        .authenticationDetails(gitlabAuthentication)
        .gitlabApiAccess(gitlabApiAccess)
        .url(configDTO.getUrl())
        .validationRepo(configDTO.getValidationRepo())
        .build();
  }

  public static io.harness.connector.entities.embedded.gitlabconnector.GitlabAuthentication buildAuthenticationDetails(
      GitAuthType gitAuthType, GitlabCredentials credentialsDTO) {
    switch (gitAuthType) {
      case SSH:
        final GitlabSshCredentials sshCredentialsDTO = (GitlabSshCredentials) credentialsDTO;
        return GitlabSshAuthentication.builder()
            .sshKeyRef(SecretRefHelper.getSecretConfigString(sshCredentialsDTO.getSshKeyRef()))
            .build();
      case HTTP:
        final GitlabHttpCredentials httpCredentialsDTO = (GitlabHttpCredentials) credentialsDTO;
        final GitlabHttpAuthenticationType type = httpCredentialsDTO.getType();
        return GitlabHttpAuthentication.builder().type(type).auth(getHttpAuth(type, httpCredentialsDTO)).build();
      default:
        throw new UnknownEnumTypeException(
            "Gitlab Auth Type", gitAuthType == null ? null : gitAuthType.getDisplayName());
    }
  }

  private static GitlabHttpAuth getHttpAuth(
      GitlabHttpAuthenticationType type, GitlabHttpCredentials httpCredentialsDTO) {
    switch (type) {
      case USERNAME_AND_PASSWORD:
        final GitlabUsernamePassword usernamePasswordDTO =
            (GitlabUsernamePassword) httpCredentialsDTO.getHttpCredentialsSpec();
        String usernameRef = getStringSecretForNullableSecret(usernamePasswordDTO.getUsernameRef());
        return io.harness.connector.entities.embedded.gitlabconnector.GitlabUsernamePassword.builder()
            .passwordRef(SecretRefHelper.getSecretConfigString(usernamePasswordDTO.getPasswordRef()))
            .username(usernamePasswordDTO.getUsername())
            .usernameRef(usernameRef)
            .build();
      case USERNAME_AND_TOKEN:
        final GitlabUsernameToken gitlabUsernameTokenDTO =
            (GitlabUsernameToken) httpCredentialsDTO.getHttpCredentialsSpec();
        String usernameReference = getStringSecretForNullableSecret(gitlabUsernameTokenDTO.getUsernameRef());
        return io.harness.connector.entities.embedded.gitlabconnector.GitlabUsernameToken.builder()
            .tokenRef(SecretRefHelper.getSecretConfigString(gitlabUsernameTokenDTO.getTokenRef()))
            .username(gitlabUsernameTokenDTO.getUsername())
            .usernameRef(usernameReference)
            .build();
      case KERBEROS:
        final GitlabKerberos gitlabKerberosDTO = (GitlabKerberos) httpCredentialsDTO.getHttpCredentialsSpec();
        return io.harness.connector.entities.embedded.gitlabconnector.GitlabKerberos.builder()
            .kerberosKeyRef(SecretRefHelper.getSecretConfigString(gitlabKerberosDTO.getKerberosKeyRef()))
            .build();
      default:
        throw new UnknownEnumTypeException("Gitlab Http Auth Type", type == null ? null : type.getDisplayName());
    }
  }
  private static String getStringSecretForNullableSecret(SecretRefData secretRefData) {
    String usernameRef = null;
    if (secretRefData != null) {
      usernameRef = SecretRefHelper.getSecretConfigString(secretRefData);
    }
    return usernameRef;
  }

  private GitlabTokenApiAccess getApiAcessByType(GitlabApiAccessSpec spec, GitlabApiAccessType apiAccessType) {
    final GitlabTokenSpec tokenSpec = (GitlabTokenSpec) spec;
    return GitlabTokenApiAccess.builder()
        .tokenRef(SecretRefHelper.getSecretConfigString(tokenSpec.getTokenRef()))
        .build();
  }

  private GitlabApiAccessType getApiAccessType(GitlabApiAccess apiAccess) {
    return apiAccess.getType();
  }

  private boolean hasApiAccess(GitlabApiAccess apiAccess) {
    return apiAccess != null;
  }

  private GitAuthType getAuthType(GitlabAuthentication authentication) {
    return authentication.getAuthType();
  }
}
