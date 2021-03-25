package io.harness.impl.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpecDTO;
import io.harness.product.ci.scm.proto.BitbucketProvider;
import io.harness.product.ci.scm.proto.GithubProvider;
import io.harness.product.ci.scm.proto.GitlabProvider;
import io.harness.product.ci.scm.proto.Provider;

import com.google.inject.Singleton;
import org.apache.commons.lang3.NotImplementedException;

@Singleton
@OwnedBy(DX)
public class ScmGitProviderMapper {
  // todo @deepak: We won't be pusing the code using the connector instead we will be using
  // source code managers, refactor this code later to use source code manager
  public Provider mapToSCMGitProvider(ScmConnector scmConnector) {
    if (scmConnector instanceof GithubConnectorDTO) {
      return mapToGithubProvider((GithubConnectorDTO) scmConnector);
    } else if (scmConnector instanceof GitlabConnectorDTO) {
      return mapToGitLabProvider((GitlabConnectorDTO) scmConnector);
    } else if (scmConnector instanceof BitbucketConnectorDTO) {
      return mapToBitbucketProvider((BitbucketConnectorDTO) scmConnector);
    } else {
      throw new NotImplementedException(
          String.format("The scm apis for the provider type %s is not supported", scmConnector.getClass()));
    }
  }

  private Provider mapToBitbucketProvider(BitbucketConnectorDTO bitbucketConnector) {
    return Provider.newBuilder().setBitbucket(createBitbucketProvider(bitbucketConnector)).build();
  }

  private BitbucketProvider createBitbucketProvider(BitbucketConnectorDTO bitbucketConnector) {
    // todo @deepak: Implement the bitbucket type, currently confused about the two options
    //  avaiable here
    return BitbucketProvider.newBuilder().build();
  }

  private Provider mapToGitLabProvider(GitlabConnectorDTO gitlabConnector) {
    return Provider.newBuilder()
        .setGitlab(createGitLabProvider(gitlabConnector))
        .setEndpoint(gitlabConnector.getUrl())
        .build();
  }

  private GitlabProvider createGitLabProvider(GitlabConnectorDTO gitlabConnector) {
    String accessToken = getAccessToken(gitlabConnector);
    return GitlabProvider.newBuilder().setPersonalToken(accessToken).build();
  }

  private String getAccessToken(GitlabConnectorDTO gitlabConnector) {
    GitlabApiAccessDTO apiAccess = gitlabConnector.getApiAccess();
    GitlabTokenSpecDTO apiAccessDTO = (GitlabTokenSpecDTO) apiAccess.getSpec();
    return String.valueOf(apiAccessDTO.getTokenRef().getDecryptedValue());
  }

  private Provider mapToGithubProvider(GithubConnectorDTO githubConnector) {
    return Provider.newBuilder().setGithub(createGithubProvider(githubConnector)).build();
  }

  private GithubProvider createGithubProvider(GithubConnectorDTO githubConnector) {
    String accessToken = getAccessToken(githubConnector);
    return GithubProvider.newBuilder().setAccessToken(accessToken).build();
  }

  private String getAccessToken(GithubConnectorDTO githubConnector) {
    GithubApiAccessDTO apiAccess = githubConnector.getApiAccess();
    GithubTokenSpecDTO apiAccessDTO = (GithubTokenSpecDTO) apiAccess.getSpec();
    return String.valueOf(apiAccessDTO.getTokenRef().getDecryptedValue());
  }
}
