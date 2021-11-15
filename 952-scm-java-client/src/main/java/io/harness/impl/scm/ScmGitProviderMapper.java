package io.harness.impl.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.utils.FieldWithPlainTextOrSecretValueHelper.getSecretAsStringFromPlainTextOrSecretRef;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cistatus.service.GithubAppConfig;
import io.harness.cistatus.service.GithubService;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccess;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernameTokenApiAccess;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccess;
import io.harness.delegate.beans.connector.scm.github.GithubAppSpec;
import io.harness.delegate.beans.connector.scm.github.GithubConnector;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpec;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccess;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnector;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpec;
import io.harness.exception.InvalidArgumentsException;
import io.harness.git.GitClientHelper;
import io.harness.product.ci.scm.proto.BitbucketCloudProvider;
import io.harness.product.ci.scm.proto.BitbucketServerProvider;
import io.harness.product.ci.scm.proto.GithubProvider;
import io.harness.product.ci.scm.proto.GitlabProvider;
import io.harness.product.ci.scm.proto.Provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Singleton
@Slf4j
@OwnedBy(DX)
public class ScmGitProviderMapper {
  @Inject(optional = true) GithubService githubService;

  public Provider mapToSCMGitProvider(ScmConnector scmConnector) {
    return mapToSCMGitProvider(scmConnector, false);
  }

  public Provider mapToSCMGitProvider(ScmConnector scmConnector, boolean debug) {
    if (scmConnector instanceof GithubConnector) {
      return mapToGithubProvider((GithubConnector) scmConnector, debug);
    } else if (scmConnector instanceof GitlabConnector) {
      return mapToGitLabProvider((GitlabConnector) scmConnector, debug);
    } else if (scmConnector instanceof BitbucketConnector) {
      return mapToBitbucketProvider((BitbucketConnector) scmConnector, debug);
    } else {
      throw new NotImplementedException(
          String.format("The scm apis for the provider type %s is not supported", scmConnector.getClass()));
    }
  }

  private Provider mapToBitbucketProvider(BitbucketConnector bitbucketConnector, boolean debug) {
    boolean skipVerify = checkScmSkipVerify();
    String bitBucketApiURL = GitClientHelper.getBitBucketApiURL(bitbucketConnector.getUrl());
    Provider.Builder builder = Provider.newBuilder().setEndpoint(bitBucketApiURL).setDebug(debug);
    if (GitClientHelper.isBitBucketSAAS(bitbucketConnector.getUrl())) {
      builder.setBitbucketCloud(createBitbucketCloudProvider(bitbucketConnector));
    } else {
      builder.setBitbucketServer(createBitbucketServerProvider(bitbucketConnector));
    }
    return builder.setSkipVerify(skipVerify).build();
  }

  private boolean checkScmSkipVerify() {
    final String scm_skip_ssl = System.getenv("SCM_SKIP_SSL");
    boolean skipVerify = "true".equals(scm_skip_ssl);
    if (skipVerify) {
      log.info("Skipping verification");
    }
    return skipVerify;
  }

  private BitbucketCloudProvider createBitbucketCloudProvider(BitbucketConnector bitbucketConnector) {
    BitbucketApiAccess apiAccess = bitbucketConnector.getApiAccess();
    BitbucketUsernameTokenApiAccess bitbucketUsernameTokenApiAccessDTO =
        (BitbucketUsernameTokenApiAccess) apiAccess.getSpec();
    String username = getSecretAsStringFromPlainTextOrSecretRef(
        bitbucketUsernameTokenApiAccessDTO.getUsername(), bitbucketUsernameTokenApiAccessDTO.getUsernameRef());
    String appPassword = String.valueOf(bitbucketUsernameTokenApiAccessDTO.getTokenRef().getDecryptedValue());

    return BitbucketCloudProvider.newBuilder().setUsername(username).setAppPassword(appPassword).build();
  }

  private BitbucketServerProvider createBitbucketServerProvider(BitbucketConnector bitbucketConnector) {
    BitbucketApiAccess apiAccess = bitbucketConnector.getApiAccess();
    BitbucketUsernameTokenApiAccess bitbucketUsernameTokenApiAccessDTO =
        (BitbucketUsernameTokenApiAccess) apiAccess.getSpec();
    String username = getSecretAsStringFromPlainTextOrSecretRef(
        bitbucketUsernameTokenApiAccessDTO.getUsername(), bitbucketUsernameTokenApiAccessDTO.getUsernameRef());
    String personalAccessToken = String.valueOf(bitbucketUsernameTokenApiAccessDTO.getTokenRef().getDecryptedValue());

    return BitbucketServerProvider.newBuilder()
        .setUsername(username)
        .setPersonalAccessToken(personalAccessToken)
        .build();
  }

  private Provider mapToGitLabProvider(GitlabConnector gitlabConnector, boolean debug) {
    boolean skipVerify = checkScmSkipVerify();
    return Provider.newBuilder()
        .setGitlab(createGitLabProvider(gitlabConnector))
        .setDebug(debug)
        .setEndpoint(GitClientHelper.getGitlabApiURL(gitlabConnector.getUrl()))
        .setSkipVerify(skipVerify)
        .build();
  }

  private GitlabProvider createGitLabProvider(GitlabConnector gitlabConnector) {
    String accessToken = getAccessToken(gitlabConnector);
    return GitlabProvider.newBuilder().setPersonalToken(accessToken).build();
  }

  private String getAccessToken(GitlabConnector gitlabConnector) {
    GitlabApiAccess apiAccess = gitlabConnector.getApiAccess();
    GitlabTokenSpec apiAccessDTO = (GitlabTokenSpec) apiAccess.getSpec();
    return String.valueOf(apiAccessDTO.getTokenRef().getDecryptedValue());
  }

  private Provider mapToGithubProvider(GithubConnector githubConnector, boolean debug) {
    boolean skipVerify = checkScmSkipVerify();
    return Provider.newBuilder()
        .setGithub(createGithubProvider(githubConnector))
        .setDebug(debug)
        .setEndpoint(GitClientHelper.getGithubApiURL(githubConnector.getUrl()))
        .setSkipVerify(skipVerify)
        .build();
  }

  private GithubProvider createGithubProvider(GithubConnector githubConnector) {
    switch (githubConnector.getApiAccess().getType()) {
      case GITHUB_APP:
        // todo @aradisavljevic: switch to scm provider for github app after it is implemented
        String token = getAccessTokenFromGithubApp(githubConnector);
        return GithubProvider.newBuilder().setAccessToken(token).build();
      case TOKEN:
        String accessToken = getAccessToken(githubConnector);
        return GithubProvider.newBuilder().setAccessToken(accessToken).build();
      default:
        throw new NotImplementedException(String.format(
            "The scm apis for the api access type %s is not supported", githubConnector.getApiAccess().getType()));
    }
  }

  private String getAccessTokenFromGithubApp(GithubConnector githubConnector) {
    GithubApiAccess apiAccess = githubConnector.getApiAccess();
    GithubAppSpec apiAccessDTO = (GithubAppSpec) apiAccess.getSpec();
    if (githubService == null) {
      throw new NotImplementedException("Token for Github App is only supported on delegate");
    }

    try {
      return githubService.getToken(GithubAppConfig.builder()
                                        .appId(apiAccessDTO.getApplicationId())
                                        .installationId(apiAccessDTO.getInstallationId())
                                        .privateKey(String.valueOf(apiAccessDTO.getPrivateKeyRef().getDecryptedValue()))
                                        .githubUrl(GitClientHelper.getGithubApiURL(githubConnector.getUrl()))
                                        .build());
    } catch (Exception ex) {
      throw new InvalidArgumentsException("Failed to generate token for github connector via git hub app ");
    }
  }

  private String getAccessToken(GithubConnector githubConnector) {
    GithubApiAccess apiAccess = githubConnector.getApiAccess();
    GithubTokenSpec apiAccessDTO = (GithubTokenSpec) apiAccess.getSpec();
    if (apiAccessDTO.getTokenRef() == null || apiAccessDTO.getTokenRef().getDecryptedValue() == null) {
      throw new InvalidArgumentsException("The personal access token is not set");
    }
    return String.valueOf(apiAccessDTO.getTokenRef().getDecryptedValue());
  }
}
