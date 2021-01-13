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
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabCredentialsDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpCredentialsSpecDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabKerberosDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabSshCredentialsSpecDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernameTokenDTO;
import io.harness.encryption.SecretRefData;

public class GitlabEntityToDTO extends ConnectorEntityToDTOMapper<GitlabConnectorDTO, GitlabConnector> {
  @Override
  public GitlabConnectorDTO createConnectorDTO(GitlabConnector connector) {
    GitlabAuthenticationDTO gitlabAuthenticationDTO = buildGitlabAuthentication(connector);
    GitlabApiAccessDTO gitlabApiAccess = null;
    if (connector.isHasApiAccess()) {
      gitlabApiAccess = buildApiAccess(connector);
    }
    return GitlabConnectorDTO.builder()
        .apiAccess(gitlabApiAccess)
        .connectionType(connector.getConnectionType())
        .authentication(gitlabAuthenticationDTO)
        .url(connector.getUrl())
        .build();
  }

  private GitlabAuthenticationDTO buildGitlabAuthentication(GitlabConnector connector) {
    final GitAuthType authType = connector.getAuthType();
    final GitlabAuthentication authenticationDetails = connector.getAuthenticationDetails();
    GitlabCredentialsDTO gitlabCredentialsDTO = null;
    switch (authType) {
      case SSH:
        final GitlabSshAuthentication gitlabSshAuthentication = (GitlabSshAuthentication) authenticationDetails;
        final GitlabSshCredentialsSpecDTO gitlabSshCredentialsSpecDTO =
            GitlabSshCredentialsSpecDTO.builder()
                .sshKeyRef(SecretRefHelper.createSecretRef(gitlabSshAuthentication.getSshKeyRef()))
                .build();
        gitlabCredentialsDTO = GitlabSshCredentialsDTO.builder().spec(gitlabSshCredentialsSpecDTO).build();
        break;
      case HTTP:
        final GitlabHttpAuthentication gitlabHttpAuthentication = (GitlabHttpAuthentication) authenticationDetails;
        final GitlabHttpAuthenticationType type = gitlabHttpAuthentication.getType();
        final GitlabHttpAuth auth = gitlabHttpAuthentication.getAuth();
        GitlabHttpCredentialsSpecDTO gitlabHttpCredentialsSpecDTO = getHttpCredentialsSpecDTO(type, auth);
        gitlabCredentialsDTO =
            GitlabHttpCredentialsDTO.builder().type(type).httpCredentialsSpec(gitlabHttpCredentialsSpecDTO).build();
    }
    return GitlabAuthenticationDTO.builder().authType(authType).credentials(gitlabCredentialsDTO).build();
  }

  private GitlabHttpCredentialsSpecDTO getHttpCredentialsSpecDTO(GitlabHttpAuthenticationType type, Object auth) {
    GitlabHttpCredentialsSpecDTO gitlabHttpCredentialsSpecDTO = null;
    switch (type) {
      case USERNAME_AND_TOKEN:
        final GitlabUsernameToken usernameToken = (GitlabUsernameToken) auth;
        SecretRefData usernameReference = null;
        if (usernameToken.getUsernameRef() != null) {
          usernameReference = SecretRefHelper.createSecretRef(usernameToken.getUsernameRef());
        }
        gitlabHttpCredentialsSpecDTO = GitlabUsernameTokenDTO.builder()
                                           .username(usernameToken.getUsername())
                                           .usernameRef(usernameReference)
                                           .tokenRef(SecretRefHelper.createSecretRef(usernameToken.getTokenRef()))
                                           .build();
        break;
      case USERNAME_AND_PASSWORD:
        final GitlabUsernamePassword gitlabUsernamePassword = (GitlabUsernamePassword) auth;
        SecretRefData usernameRef = null;
        if (gitlabUsernamePassword.getUsernameRef() != null) {
          usernameRef = SecretRefHelper.createSecretRef(gitlabUsernamePassword.getUsernameRef());
        }
        gitlabHttpCredentialsSpecDTO =
            GitlabUsernamePasswordDTO.builder()
                .passwordRef(SecretRefHelper.createSecretRef(gitlabUsernamePassword.getPasswordRef()))
                .username(gitlabUsernamePassword.getUsername())
                .usernameRef(usernameRef)
                .build();
        break;
      case KERBEROS:
        final GitlabKerberos gitlabKerberos = (GitlabKerberos) auth;
        gitlabHttpCredentialsSpecDTO =
            GitlabKerberosDTO.builder()
                .kerberosKeyRef(SecretRefHelper.createSecretRef(gitlabKerberos.getKerberosKeyRef()))
                .build();
        break;
    }
    return gitlabHttpCredentialsSpecDTO;
  }

  private GitlabApiAccessDTO buildApiAccess(GitlabConnector connector) {
    final GitlabTokenApiAccess gitlabTokenApiAccess = connector.getGitlabApiAccess();
    final GitlabTokenSpecDTO gitlabTokenSpecDTO =
        GitlabTokenSpecDTO.builder()
            .tokenRef(SecretRefHelper.createSecretRef(gitlabTokenApiAccess.getTokenRef()))
            .build();
    return GitlabApiAccessDTO.builder().type(GitlabApiAccessType.TOKEN).spec(gitlabTokenSpecDTO).build();
  }
}
