package io.harness.impl.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernameTokenApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpecDTO;
import io.harness.gitsync.common.impl.GitUtils;
import io.harness.product.ci.scm.proto.BitbucketCloudProvider;
import io.harness.product.ci.scm.proto.BitbucketServerProvider;
import io.harness.product.ci.scm.proto.GithubProvider;
import io.harness.product.ci.scm.proto.GitlabProvider;
import io.harness.product.ci.scm.proto.Provider;
import io.harness.utils.FieldWithPlainTextOrSecretValueHelper;

import com.google.inject.Singleton;
import java.util.Arrays;
import org.apache.commons.lang3.NotImplementedException;

@Singleton
@OwnedBy(DX)
public class ScmGitProviderMapper {
  public Provider mapToSCMGitProvider(ScmConnector scmConnector) {
    final Provider.Builder providerBuilder = Provider.newBuilder();
    if (!GitUtils.isSaasGit(scmConnector.getUrl()).isSaasGit()) {
      providerBuilder.setEndpoint(scmConnector.getUrl());
    }
    if (scmConnector instanceof GithubConnectorDTO) {
      return providerBuilder.setGithub(createGithubProvider((GithubConnectorDTO) scmConnector)).build();
    } else if (scmConnector instanceof GitlabConnectorDTO) {
      return providerBuilder.setGitlab(createGitLabProvider((GitlabConnectorDTO) scmConnector)).build();
    } else if (scmConnector instanceof BitbucketConnectorDTO) {
      if (GitUtils.isSaasGit(scmConnector.getUrl()).isSaasGit()) {
        return providerBuilder.setBitbucketCloud(createBitbucketCloudProvider((BitbucketConnectorDTO) scmConnector))
            .build();
      } else {
        return providerBuilder.setBitbucketServer(createBitbucketServerProvider((BitbucketConnectorDTO) scmConnector))
            .build();
      }
    } else {
      throw new NotImplementedException(
          String.format("The scm apis for the provider type %s is not supported", scmConnector.getClass()));
    }
  }

  private BitbucketCloudProvider createBitbucketCloudProvider(BitbucketConnectorDTO bitbucketConnector) {
    final BitbucketUsernameTokenApiAccessDTO bitbucketUsernameTokenApiAccessDTO =
        (BitbucketUsernameTokenApiAccessDTO) bitbucketConnector.getApiAccess().getSpec();
    String username = FieldWithPlainTextOrSecretValueHelper.getSecretAsStringFromPlainTextOrSecretRef(
        bitbucketUsernameTokenApiAccessDTO.getUsername(), bitbucketUsernameTokenApiAccessDTO.getUsernameRef());
    return BitbucketCloudProvider.newBuilder()
        .setUsername(username)
        .setAppPassword(Arrays.toString(bitbucketUsernameTokenApiAccessDTO.getTokenRef().getDecryptedValue()))
        .build();
  }

  private BitbucketServerProvider createBitbucketServerProvider(BitbucketConnectorDTO bitbucketConnector) {
    final BitbucketUsernameTokenApiAccessDTO bitbucketUsernameTokenApiAccessDTO =
        (BitbucketUsernameTokenApiAccessDTO) bitbucketConnector.getApiAccess().getSpec();
    String username = FieldWithPlainTextOrSecretValueHelper.getSecretAsStringFromPlainTextOrSecretRef(
        bitbucketUsernameTokenApiAccessDTO.getUsername(), bitbucketUsernameTokenApiAccessDTO.getUsernameRef());
    return BitbucketServerProvider.newBuilder()
        .setUsername(username)
        .setPersonalAccessToken(Arrays.toString(bitbucketUsernameTokenApiAccessDTO.getTokenRef().getDecryptedValue()))
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
