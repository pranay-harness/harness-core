package io.harness.delegate.task.ci;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessType.TOKEN;

import static java.lang.String.format;

import io.harness.cistatus.service.GithubAppConfig;
import io.harness.cistatus.service.GithubService;
import io.harness.cistatus.service.bitbucket.BitbucketConfig;
import io.harness.cistatus.service.bitbucket.BitbucketService;
import io.harness.cistatus.service.gitlab.GitlabConfig;
import io.harness.cistatus.service.gitlab.GitlabService;
import io.harness.cistatus.service.gitlab.GitlabServiceImpl;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.status.BuildStatusPushResponse;
import io.harness.delegate.beans.ci.status.BuildStatusPushResponse.Status;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernameTokenApiAccess;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccess;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessType;
import io.harness.delegate.beans.connector.scm.github.GithubAppSpec;
import io.harness.delegate.beans.connector.scm.github.GithubConnector;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpec;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnector;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpec;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.ci.CIBuildPushParameters.CIBuildPushTaskType;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.git.GitClientHelper;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class CIBuildStatusPushTask extends AbstractDelegateRunnableTask {
  public static final String TARGET_URL = "target_url";
  @Inject private GithubService githubService;
  @Inject private BitbucketService bitbucketService;
  @Inject private GitlabService gitlabService;
  @Inject private SecretDecryptionService secretDecryptionService;

  private static final String DESC = "description";
  private static final String STATE = "state";
  private static final String URL = "url";
  private static final String CONTEXT = "context";
  private static final String DETAILS_URL = "details_url";

  private static final String BITBUCKET_KEY = "key";
  private static final String GITHUB_API_URL = "https://api.github.com/";
  private static final String BITBUCKET_API_URL = "https://api.bitbucket.org/";
  private static final String GITLAB_API_URL = "https://gitlab.com/api/";
  private static final String APP_URL = "https://app.harness.io";

  public CIBuildStatusPushTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    if (((CIBuildPushParameters) parameters).commandType == CIBuildPushTaskType.STATUS) {
      try {
        CIBuildStatusPushParameters ciBuildStatusPushParameters = (CIBuildStatusPushParameters) parameters;
        boolean statusSent = false;
        if (ciBuildStatusPushParameters.getGitSCMType() == GitSCMType.GITHUB) {
          statusSent = sendBuildStatusToGitHub(ciBuildStatusPushParameters);
        } else if (ciBuildStatusPushParameters.getGitSCMType() == GitSCMType.BITBUCKET) {
          statusSent = sendBuildStatusToBitbucket(ciBuildStatusPushParameters);
        } else if (ciBuildStatusPushParameters.getGitSCMType() == GitSCMType.GITLAB) {
          statusSent = sendBuildStatusToGitLab(ciBuildStatusPushParameters);
        } else {
          throw new UnsupportedOperationException("Not supported");
        }

        if (statusSent) {
          log.info("Successfully sent the git status for sha {}, stage identifier {}",
              ciBuildStatusPushParameters.getSha(), ciBuildStatusPushParameters.getIdentifier());
          return BuildStatusPushResponse.builder().status(Status.SUCCESS).build();
        } else {
          return BuildStatusPushResponse.builder().status(Status.ERROR).build();
        }
      } catch (Exception ex) {
        log.error("failed to send status", ex);
        return BuildStatusPushResponse.builder().status(Status.ERROR).build();
      }
    }
    return BuildStatusPushResponse.builder().status(Status.ERROR).build();
  }

  private GithubAppSpec retrieveGithubAppSpecDTO(GithubConnector gitConfigDTO, ConnectorDetails connectorDetails) {
    GithubApiAccess githubApiAccess = gitConfigDTO.getApiAccess();
    if (githubApiAccess.getType() == GithubApiAccessType.GITHUB_APP) {
      GithubAppSpec githubAppSpec = (GithubAppSpec) githubApiAccess.getSpec();
      secretDecryptionService.decrypt(githubAppSpec, connectorDetails.getEncryptedDataDetails());
      return githubAppSpec;
    } else {
      throw new CIStageExecutionException(
          format("Unsupported access type %s for github status", githubApiAccess.getType()));
    }
  }

  private boolean sendBuildStatusToGitHub(CIBuildStatusPushParameters ciBuildStatusPushParameters) {
    GithubConnector gitConfigDTO =
        (GithubConnector) ciBuildStatusPushParameters.getConnectorDetails().getConnectorConfig();

    GithubApiAccess githubApiAccess = gitConfigDTO.getApiAccess();

    if (githubApiAccess == null) {
      log.warn("Not sending status because api access is not enabled for sha {}", ciBuildStatusPushParameters.getSha());
      return false;
    }

    String token = null;
    if (githubApiAccess.getType() == GithubApiAccessType.GITHUB_APP) {
      GithubAppSpec githubAppSpec =
          retrieveGithubAppSpecDTO(gitConfigDTO, ciBuildStatusPushParameters.getConnectorDetails());

      GithubAppConfig githubAppConfig =
          GithubAppConfig.builder()
              .installationId(githubAppSpec.getInstallationId())
              .appId(githubAppSpec.getApplicationId())
              .privateKey(new String(githubAppSpec.getPrivateKeyRef().getDecryptedValue()))
              .githubUrl(getGitApiURL(gitConfigDTO.getUrl()))
              .build();
      token = githubService.getToken(githubAppConfig);
      if (EmptyPredicate.isEmpty(token)) {
        log.error("Not sending status because token is empty for appId {}, installationId {}, sha {}",
            githubAppSpec.getApplicationId(), githubAppSpec.getInstallationId(), ciBuildStatusPushParameters.getSha());
        return false;
      }
    } else if (githubApiAccess.getType() == GithubApiAccessType.TOKEN) {
      GithubTokenSpec githubTokenSpec = (GithubTokenSpec) githubApiAccess.getSpec();
      secretDecryptionService.decrypt(
          githubTokenSpec, ciBuildStatusPushParameters.getConnectorDetails().getEncryptedDataDetails());
      token = new String(githubTokenSpec.getTokenRef().getDecryptedValue());

      if (EmptyPredicate.isEmpty(token)) {
        log.error("Not sending status because token is empty for sha {}", ciBuildStatusPushParameters.getSha());
        return false;
      }
    } else {
      throw new CIStageExecutionException(
          format("Unsupported access type %s for github status", githubApiAccess.getType()));
    }

    if (isNotEmpty(token)) {
      Map<String, Object> bodyObjectMap = new HashMap<>();
      bodyObjectMap.put(DESC, ciBuildStatusPushParameters.getDesc());
      bodyObjectMap.put(CONTEXT, ciBuildStatusPushParameters.getIdentifier());
      bodyObjectMap.put(STATE, ciBuildStatusPushParameters.getState());
      bodyObjectMap.put(TARGET_URL, ciBuildStatusPushParameters.getDetailsUrl());
      // TODO Sending Just URL will require refactoring in sendStatus method, Will be done POST CI GA
      GithubAppConfig githubAppConfig =
          GithubAppConfig.builder().githubUrl(getGitApiURL(gitConfigDTO.getUrl())).build();

      return githubService.sendStatus(githubAppConfig, token, ciBuildStatusPushParameters.getSha(),
          ciBuildStatusPushParameters.getOwner(), ciBuildStatusPushParameters.getRepo(), bodyObjectMap);
    } else {
      log.error("Not sending status because token is empty for sha {}", ciBuildStatusPushParameters.getSha());
      return false;
    }
  }

  private String getGitApiURL(String url) {
    if (GitClientHelper.isGithubSAAS(url)) {
      return GITHUB_API_URL;
    } else {
      String domain = GitClientHelper.getGitSCM(url);
      return "https://" + domain + "/api/v3/";
    }
  }
  private String getBitBucketApiURL(String url) {
    if (url.contains("bitbucket.org/")) {
      return BITBUCKET_API_URL;
    } else {
      String domain = GitClientHelper.getGitSCM(url);
      return "https://" + domain + "/";
    }
  }

  private String getGitlabApiURL(String url) {
    if (url.contains("gitlab.com")) {
      return GITLAB_API_URL;
    } else {
      String domain = GitClientHelper.getGitSCM(url);
      return "https://" + domain + "/api/";
    }
  }
  private boolean sendBuildStatusToBitbucket(CIBuildStatusPushParameters ciBuildStatusPushParameters) {
    Map<String, Object> bodyObjectMap = new HashMap<>();
    bodyObjectMap.put(DESC, ciBuildStatusPushParameters.getDesc());
    bodyObjectMap.put(BITBUCKET_KEY, ciBuildStatusPushParameters.getIdentifier());
    bodyObjectMap.put(STATE, ciBuildStatusPushParameters.getState());
    bodyObjectMap.put(URL, ciBuildStatusPushParameters.getDetailsUrl());

    String token = retrieveAuthToken(
        ciBuildStatusPushParameters.getGitSCMType(), ciBuildStatusPushParameters.getConnectorDetails());

    BitbucketConnector gitConfigDTO =
        (BitbucketConnector) ciBuildStatusPushParameters.getConnectorDetails().getConnectorConfig();

    if (isNotEmpty(token)) {
      return bitbucketService.sendStatus(
          BitbucketConfig.builder().bitbucketUrl(getBitBucketApiURL(gitConfigDTO.getUrl())).build(),
          ciBuildStatusPushParameters.getUserName(), token, null, ciBuildStatusPushParameters.getSha(),
          ciBuildStatusPushParameters.getOwner(), ciBuildStatusPushParameters.getRepo(), bodyObjectMap);
    } else {
      log.error("Not sending status because token is empty sha {}", ciBuildStatusPushParameters.getSha());
      return false;
    }
  }

  private boolean sendBuildStatusToGitLab(CIBuildStatusPushParameters ciBuildStatusPushParameters) {
    Map<String, Object> bodyObjectMap = new HashMap<>();
    bodyObjectMap.put(GitlabServiceImpl.DESC, ciBuildStatusPushParameters.getDesc());
    bodyObjectMap.put(GitlabServiceImpl.CONTEXT, ciBuildStatusPushParameters.getIdentifier());
    bodyObjectMap.put(GitlabServiceImpl.STATE, ciBuildStatusPushParameters.getState());
    bodyObjectMap.put(GitlabServiceImpl.TARGET_URL, ciBuildStatusPushParameters.getDetailsUrl());

    String token = retrieveAuthToken(
        ciBuildStatusPushParameters.getGitSCMType(), ciBuildStatusPushParameters.getConnectorDetails());

    GitlabConnector gitConfigDTO =
        (GitlabConnector) ciBuildStatusPushParameters.getConnectorDetails().getConnectorConfig();

    if (isNotEmpty(token)) {
      return gitlabService.sendStatus(GitlabConfig.builder().gitlabUrl(getGitlabApiURL(gitConfigDTO.getUrl())).build(),
          ciBuildStatusPushParameters.getUserName(), token, null, ciBuildStatusPushParameters.getSha(),
          ciBuildStatusPushParameters.getOwner(), ciBuildStatusPushParameters.getRepo(), bodyObjectMap);
    } else {
      log.error("Not sending status because token is empty sha {}", ciBuildStatusPushParameters.getSha());
      return false;
    }
  }

  private String retrieveAuthToken(GitSCMType gitSCMType, ConnectorDetails gitConnector) {
    switch (gitSCMType) {
      case GITHUB:
        return ""; // It does not require token because auth occurs via github app
      case GITLAB:
        GitlabConnector gitConfigDTO = (GitlabConnector) gitConnector.getConnectorConfig();
        if (gitConfigDTO.getApiAccess() == null) {
          throw new CIStageExecutionException(
              format("Failed to retrieve token info for gitlab connector: ", gitConnector.getIdentifier()));
        }
        if (gitConfigDTO.getApiAccess().getType() == TOKEN) {
          GitlabTokenSpec gitlabTokenSpecDTO = (GitlabTokenSpec) gitConfigDTO.getApiAccess().getSpec();
          secretDecryptionService.decrypt(gitlabTokenSpecDTO, gitConnector.getEncryptedDataDetails());
          return new String(gitlabTokenSpecDTO.getTokenRef().getDecryptedValue());
        } else {
          throw new CIStageExecutionException(
              format("Unsupported access type %s for gitlab status", gitConfigDTO.getApiAccess().getType()));
        }
      case BITBUCKET:
        BitbucketConnector bitbucketConnector = (BitbucketConnector) gitConnector.getConnectorConfig();
        if (bitbucketConnector.getApiAccess() == null) {
          throw new CIStageExecutionException(
              format("Failed to retrieve token info for Bitbucket connector: %s", gitConnector.getIdentifier()));
        }
        if (bitbucketConnector.getApiAccess().getType() == BitbucketApiAccessType.USERNAME_AND_TOKEN) {
          BitbucketUsernameTokenApiAccess bitbucketTokenSpecDTO =
              (BitbucketUsernameTokenApiAccess) bitbucketConnector.getApiAccess().getSpec();
          secretDecryptionService.decrypt(bitbucketTokenSpecDTO, gitConnector.getEncryptedDataDetails());
          return new String(bitbucketTokenSpecDTO.getTokenRef().getDecryptedValue());
        } else {
          throw new CIStageExecutionException(
              format("Unsupported access type %s for gitlab status", bitbucketConnector.getApiAccess().getType()));
        }

      default:
        throw new CIStageExecutionException(format("Unsupported scm type %s for git status", gitSCMType));
    }
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }
}
