package io.harness.delegate.beans.connector.scm.adapter;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnector;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.connector.scm.github.GithubConnector;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnector;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class ScmConnectorMapper {
  public static GitConfig toGitConfigDTO(ScmConnector scmConnector) {
    if (scmConnector instanceof GithubConnector) {
      return GithubToGitMapper.mapToGitConfigDTO((GithubConnector) scmConnector);
    } else if (scmConnector instanceof GitlabConnector) {
      return GitlabToGitMapper.mapToGitConfigDTO((GitlabConnector) scmConnector);
    } else if (scmConnector instanceof BitbucketConnector) {
      return BitbucketToGitMapper.mapToGitConfigDTO((BitbucketConnector) scmConnector);
    } else {
      return (GitConfig) scmConnector;
    }
  }
}
