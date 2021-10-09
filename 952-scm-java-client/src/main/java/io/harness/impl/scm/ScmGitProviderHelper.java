package io.harness.impl.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.git.GitClientHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.NotImplementedException;

@Singleton
@OwnedBy(DX)
public class ScmGitProviderHelper {
  @Inject GitClientHelper gitClientHelper;

  public String getSlug(ScmConnector scmConnector) {
    if (scmConnector instanceof GithubConnectorDTO) {
      return getSlugFromUrl(((GithubConnectorDTO) scmConnector).getUrl());
    } else if (scmConnector instanceof GitlabConnectorDTO) {
      return getSlugFromUrl(((GitlabConnectorDTO) scmConnector).getUrl());
    } else if (scmConnector instanceof BitbucketConnectorDTO) {
      return getSlugFromUrl(((BitbucketConnectorDTO) scmConnector).getUrl());
    } else {
      throw new NotImplementedException(
          String.format("The scm apis for the provider type %s is not supported", scmConnector.getClass()));
    }
  }

  private String getSlugFromUrl(String url) {
    String repoName = gitClientHelper.getGitRepo(url);
    String ownerName = gitClientHelper.getGitOwner(url, false);
    return ownerName + "/" + repoName;
  }
}
