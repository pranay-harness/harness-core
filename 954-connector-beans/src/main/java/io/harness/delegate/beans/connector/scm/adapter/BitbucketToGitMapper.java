package io.harness.delegate.beans.connector.scm.adapter;

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

import lombok.experimental.UtilityClass;

@UtilityClass
public class BitbucketToGitMapper {
  public static GitConfigDTO mapToGitConfigDTO(BitbucketConnectorDTO bitbucketConnectorDTO) {
    final GitAuthType authType = bitbucketConnectorDTO.getAuthentication().getAuthType();
    final GitConnectionType connectionType = bitbucketConnectorDTO.getConnectionType();
    final String url = bitbucketConnectorDTO.getUrl();
    if (authType == GitAuthType.HTTP) {
      final BitbucketHttpCredentialsSpecDTO httpCredentialsSpec =
          ((BitbucketHttpCredentialsDTO) bitbucketConnectorDTO.getAuthentication().getCredentials())
              .getHttpCredentialsSpec();
      final BitbucketUsernamePasswordDTO usernamePasswordDTO = (BitbucketUsernamePasswordDTO) httpCredentialsSpec;
      return GitConfigCreater.getGitConfigForHttp(connectionType, url, usernamePasswordDTO.getUsername(),
          usernamePasswordDTO.getUsernameRef(), usernamePasswordDTO.getPasswordRef());
    } else if (authType == GitAuthType.SSH) {
      final BitbucketSshCredentialsDTO sshCredentials =
          (BitbucketSshCredentialsDTO) bitbucketConnectorDTO.getAuthentication().getCredentials();
      final SecretRefData sshKeyRef = sshCredentials.getSshKeyRef();
      return GitConfigCreater.getGitConfigForSsh(connectionType, url, sshKeyRef);
    }
    throw new InvalidRequestException("Unknown auth type: " + authType);
  }
}
