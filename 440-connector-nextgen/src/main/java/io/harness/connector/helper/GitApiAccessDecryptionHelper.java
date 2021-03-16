package io.harness.connector.helper;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;

@Singleton
public class GitApiAccessDecryptionHelper {
  public DecryptableEntity getAPIAccessDecryptableEntity(GithubConnectorDTO githubConnectorDTO) {
    if (githubConnectorDTO == null || githubConnectorDTO.getApiAccess() == null) {
      throw new InvalidRequestException("The given connector doesn't have api access field set");
    }
    return githubConnectorDTO.getApiAccess().getSpec();
  }

  public DecryptableEntity getAPIAccessDecryptableEntity(BitbucketConnectorDTO bitbucketConnectorDTO) {
    if (bitbucketConnectorDTO == null || bitbucketConnectorDTO.getApiAccess() == null) {
      throw new InvalidRequestException("The given connector doesn't have api access field set");
    }
    return bitbucketConnectorDTO.getApiAccess().getSpec();
  }

  public DecryptableEntity getAPIAccessDecryptableEntity(GitlabConnectorDTO gitlabConnectorDTO) {
    if (gitlabConnectorDTO == null || gitlabConnectorDTO.getApiAccess() == null) {
      throw new InvalidRequestException("The given connector doesn't have api access field set");
    }
    return gitlabConnectorDTO.getApiAccess().getSpec();
  }
}
