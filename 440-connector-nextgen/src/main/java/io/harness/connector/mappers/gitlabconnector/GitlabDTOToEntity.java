package io.harness.connector.mappers.gitlabconnector;

import io.harness.connector.entities.embedded.gitlabconnector.GitlabAuthentication;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabHttpAuth;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabHttpAuthentication;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabKerberos;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabSshAuthentication;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabTokenApiAccess;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabUsernamePassword;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabUsernameToken;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessSpecDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabCredentialsDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabKerberosDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernameTokenDTO;
import io.harness.encryption.SecretRefData;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.ng.core.NGAccess;
import io.harness.ng.service.SecretRefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class GitlabDTOToEntity implements ConnectorDTOToEntityMapper<GitlabConnectorDTO, GitlabConnector> {
  private SecretRefService secretRefService;

  @Override
  public GitlabConnector toConnectorEntity(GitlabConnectorDTO configDTO, NGAccess ngAccess) {
    GitAuthType gitAuthType = getAuthType(configDTO.getAuthentication());
    GitlabAuthentication gitlabAuthentication =
        buildAuthenticationDetails(configDTO.getAuthentication().getCredentials(), gitAuthType, ngAccess);
    boolean hasApiAccess = hasApiAccess(configDTO.getApiAccess());
    GitlabApiAccessType apiAccessType = null;
    GitlabTokenApiAccess gitlabApiAccess = null;
    if (hasApiAccess) {
      apiAccessType = getApiAccessType(configDTO.getApiAccess());
      gitlabApiAccess = getApiAcessByType(configDTO.getApiAccess().getSpec(), apiAccessType, ngAccess);
    }
    return GitlabConnector.builder()
        .connectionType(configDTO.getConnectionType())
        .authType(gitAuthType)
        .hasApiAccess(hasApiAccess)
        .authenticationDetails(gitlabAuthentication)
        .gitlabApiAccess(gitlabApiAccess)
        .url(configDTO.getUrl())
        .build();
  }

  private GitlabAuthentication buildAuthenticationDetails(
      GitlabCredentialsDTO credentialsDTO, GitAuthType gitAuthType, NGAccess ngAccess) {
    switch (gitAuthType) {
      case SSH:
        final GitlabSshCredentialsDTO sshCredentialsDTO = (GitlabSshCredentialsDTO) credentialsDTO;
        return GitlabSshAuthentication.builder()
            .sshKeyRef(secretRefService.validateAndGetSecretConfigString(sshCredentialsDTO.getSshKeyRef(), ngAccess))
            .build();
      case HTTP:
        final GitlabHttpCredentialsDTO httpCredentialsDTO = (GitlabHttpCredentialsDTO) credentialsDTO;
        final GitlabHttpAuthenticationType type = httpCredentialsDTO.getType();
        return GitlabHttpAuthentication.builder()
            .type(type)
            .auth(getHttpAuth(type, httpCredentialsDTO, ngAccess))
            .build();
      default:
        throw new UnknownEnumTypeException(
            "Gitlab Auth Type", gitAuthType == null ? null : gitAuthType.getDisplayName());
    }
  }

  private GitlabHttpAuth getHttpAuth(
      GitlabHttpAuthenticationType type, GitlabHttpCredentialsDTO httpCredentialsDTO, NGAccess ngAccess) {
    switch (type) {
      case USERNAME_AND_PASSWORD:
        final GitlabUsernamePasswordDTO usernamePasswordDTO =
            (GitlabUsernamePasswordDTO) httpCredentialsDTO.getHttpCredentialsSpec();
        String usernameRef = getStringSecretForNullableSecret(usernamePasswordDTO.getUsernameRef(), ngAccess);
        return GitlabUsernamePassword.builder()
            .passwordRef(
                secretRefService.validateAndGetSecretConfigString(usernamePasswordDTO.getPasswordRef(), ngAccess))
            .username(usernamePasswordDTO.getUsername())
            .usernameRef(usernameRef)
            .build();
      case USERNAME_AND_TOKEN:
        final GitlabUsernameTokenDTO gitlabUsernameTokenDTO =
            (GitlabUsernameTokenDTO) httpCredentialsDTO.getHttpCredentialsSpec();
        String usernameReference = getStringSecretForNullableSecret(gitlabUsernameTokenDTO.getUsernameRef(), ngAccess);
        return GitlabUsernameToken.builder()
            .tokenRef(secretRefService.validateAndGetSecretConfigString(gitlabUsernameTokenDTO.getTokenRef(), ngAccess))
            .username(gitlabUsernameTokenDTO.getUsername())
            .usernameRef(usernameReference)
            .build();
      case KERBEROS:
        final GitlabKerberosDTO gitlabKerberosDTO = (GitlabKerberosDTO) httpCredentialsDTO.getHttpCredentialsSpec();
        return GitlabKerberos.builder()
            .kerberosKeyRef(
                secretRefService.validateAndGetSecretConfigString(gitlabKerberosDTO.getKerberosKeyRef(), ngAccess))
            .build();
      default:
        throw new UnknownEnumTypeException("Gitlab Http Auth Type", type == null ? null : type.getDisplayName());
    }
  }
  private String getStringSecretForNullableSecret(SecretRefData secretRefData, NGAccess ngAccess) {
    String usernameRef = null;
    if (secretRefData != null) {
      usernameRef = secretRefService.validateAndGetSecretConfigString(secretRefData, ngAccess);
    }
    return usernameRef;
  }

  private GitlabTokenApiAccess getApiAcessByType(
      GitlabApiAccessSpecDTO spec, GitlabApiAccessType apiAccessType, NGAccess ngAccess) {
    final GitlabTokenSpecDTO tokenSpec = (GitlabTokenSpecDTO) spec;
    return GitlabTokenApiAccess.builder()
        .tokenRef(secretRefService.validateAndGetSecretConfigString(tokenSpec.getTokenRef(), ngAccess))
        .build();
  }

  private GitlabApiAccessType getApiAccessType(GitlabApiAccessDTO apiAccess) {
    return apiAccess.getType();
  }

  private boolean hasApiAccess(GitlabApiAccessDTO apiAccess) {
    return apiAccess != null;
  }

  private GitAuthType getAuthType(GitlabAuthenticationDTO authentication) {
    return authentication.getAuthType();
  }
}
