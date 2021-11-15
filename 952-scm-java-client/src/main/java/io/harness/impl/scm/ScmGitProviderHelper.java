package io.harness.impl.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnector;
import io.harness.delegate.beans.connector.scm.github.GithubConnector;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnector;
import io.harness.git.GitClientHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

@Singleton
@OwnedBy(DX)
public class ScmGitProviderHelper {
  @Inject GitClientHelper gitClientHelper;

  public String getSlug(ScmConnector scmConnector) {
    if (scmConnector instanceof GithubConnector) {
      return getSlugFromUrl(((GithubConnector) scmConnector).getUrl());
    } else if (scmConnector instanceof GitlabConnector) {
      return getSlugFromUrl(((GitlabConnector) scmConnector).getUrl());
    } else if (scmConnector instanceof BitbucketConnector) {
      return getSlugFromUrlForBitbucket(((BitbucketConnector) scmConnector).getUrl());
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

  private String getSlugFromUrlForBitbucket(String url) {
    String repoName = gitClientHelper.getGitRepo(url);
    String ownerName = gitClientHelper.getGitOwner(url, false);
    if (!GitClientHelper.isBitBucketSAAS(url)) {
      if (ownerName.equals("scm")) {
        return repoName;
      }
      if (repoName.startsWith("scm/")) {
        return StringUtils.removeStart(repoName, "scm/");
      }
    }
    return ownerName + "/" + repoName;
  }
}
