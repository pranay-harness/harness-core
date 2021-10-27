package io.harness.delegate.beans.connector.scm.adapter;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameTokenDTO;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class GithubToGitMapper {
  public static GitConfigDTO mapToGitConfigDTO(GithubConnectorDTO githubConnectorDTO) {
    final GitAuthType authType = githubConnectorDTO.getAuthentication().getAuthType();
    final GitConnectionType connectionType = githubConnectorDTO.getConnectionType();
    final String url = githubConnectorDTO.getUrl();
    final String validationRepo = githubConnectorDTO.getValidationRepo();

    //    GitConfigDTO.GitConfigDTOBuilder resultGitConfigBuilder =
    //        GitConfigDTO.builder()
    //            .executeOnManager(githubConnectorDTO.getExecuteOnManager())
    //            .delegateSelectors(githubConnectorDTO.getDelegateSelectors())
    //            .url(url)
    //            .validationRepo(validationRepo);
    if (authType == GitAuthType.HTTP) {
      final GithubHttpCredentialsDTO credentials =
          (GithubHttpCredentialsDTO) githubConnectorDTO.getAuthentication().getCredentials();
      String username;
      SecretRefData usernameRef, passwordRef;
      if (credentials.getType() == GithubHttpAuthenticationType.USERNAME_AND_PASSWORD) {
        final GithubUsernamePasswordDTO httpCredentialsSpec =
            (GithubUsernamePasswordDTO) credentials.getHttpCredentialsSpec();
        username = httpCredentialsSpec.getUsername();
        usernameRef = httpCredentialsSpec.getUsernameRef();
        passwordRef = httpCredentialsSpec.getPasswordRef();
      } else {
        final GithubUsernameTokenDTO githubUsernameTokenDTO =
            (GithubUsernameTokenDTO) credentials.getHttpCredentialsSpec();
        username = githubUsernameTokenDTO.getUsername();
        usernameRef = githubUsernameTokenDTO.getUsernameRef();
        passwordRef = githubUsernameTokenDTO.getTokenRef();
      }
      GitConfigDTO gitConfigForHttp = GitConfigCreater.getGitConfigForHttp(connectionType, url, validationRepo,
          username, usernameRef, passwordRef, githubConnectorDTO.getDelegateSelectors());
      gitConfigForHttp.setExecuteOnManager(githubConnectorDTO.getExecuteOnManager());
      return gitConfigForHttp;

    } else if (authType == GitAuthType.SSH) {
      final GithubSshCredentialsDTO credentials =
          (GithubSshCredentialsDTO) githubConnectorDTO.getAuthentication().getCredentials();
      final SecretRefData sshKeyRef = credentials.getSshKeyRef();
      return GitConfigCreater.getGitConfigForSsh(
          connectionType, url, validationRepo, sshKeyRef, githubConnectorDTO.getDelegateSelectors());
    }
    throw new InvalidRequestException("Unknown auth type: " + authType);
  }
}
