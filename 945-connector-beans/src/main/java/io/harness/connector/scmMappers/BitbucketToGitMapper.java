package io.harness.connector.scmMappers;

import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentialsSpecDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BitbucketToGitMapper {
  @Inject GitConfigCreater gitConfigCreater;

  public GitConfigDTO mapToGitConfigDTO(BitbucketConnectorDTO bitbucketConnectorDTO) {
    final GitAuthType authType = bitbucketConnectorDTO.getAuthentication().getAuthType();
    final GitConnectionType connectionType = bitbucketConnectorDTO.getConnectionType();
    final String url = bitbucketConnectorDTO.getUrl();
    if (authType == GitAuthType.HTTP) {
      final BitbucketHttpCredentialsSpecDTO httpCredentialsSpec =
          ((BitbucketHttpCredentialsDTO) bitbucketConnectorDTO.getAuthentication().getCredentials())
              .getHttpCredentialsSpec();
      final BitbucketUsernamePasswordDTO usernamePasswordDTO = (BitbucketUsernamePasswordDTO) httpCredentialsSpec;
      return gitConfigCreater.getGitConfigForHttp(connectionType, url, usernamePasswordDTO.getUsername(),
          usernamePasswordDTO.getUsernameRef(), usernamePasswordDTO.getPasswordRef());
    } else if (authType == GitAuthType.SSH) {
      final BitbucketSshCredentialsDTO sshCredentials =
          (BitbucketSshCredentialsDTO) bitbucketConnectorDTO.getAuthentication().getCredentials();
      final SecretRefData sshKeyRef = sshCredentials.getSpec().getSshKeyRef();
      return gitConfigCreater.getGitConfigForSsh(connectionType, url, sshKeyRef);
    }
    throw new InvalidRequestException("Unknown auth type: " + authType);
  }
}
