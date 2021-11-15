package io.harness.delegate.beans.connector.scm.adapter;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentials;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentialsSpec;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketSshCredentials;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernamePassword;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class BitbucketToGitMapper {
  public static GitConfig mapToGitConfigDTO(BitbucketConnector bitbucketConnector) {
    final GitAuthType authType = bitbucketConnector.getAuthentication().getAuthType();
    final GitConnectionType connectionType = bitbucketConnector.getConnectionType();
    final String url = bitbucketConnector.getUrl();
    final String validationRepo = bitbucketConnector.getValidationRepo();
    if (authType == GitAuthType.HTTP) {
      final BitbucketHttpCredentialsSpec httpCredentialsSpec =
          ((BitbucketHttpCredentials) bitbucketConnector.getAuthentication().getCredentials()).getHttpCredentialsSpec();
      final BitbucketUsernamePassword usernamePasswordDTO = (BitbucketUsernamePassword) httpCredentialsSpec;
      return GitConfigCreater.getGitConfigForHttp(connectionType, url, validationRepo,
          usernamePasswordDTO.getUsername(), usernamePasswordDTO.getUsernameRef(), usernamePasswordDTO.getPasswordRef(),
          bitbucketConnector.getDelegateSelectors());
    } else if (authType == GitAuthType.SSH) {
      final BitbucketSshCredentials sshCredentials =
          (BitbucketSshCredentials) bitbucketConnector.getAuthentication().getCredentials();
      final SecretRefData sshKeyRef = sshCredentials.getSshKeyRef();
      return GitConfigCreater.getGitConfigForSsh(
          connectionType, url, validationRepo, sshKeyRef, bitbucketConnector.getDelegateSelectors());
    }
    throw new InvalidRequestException("Unknown auth type: " + authType);
  }
}
