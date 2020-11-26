package io.harness.delegate.task.ci;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cistatus.service.GithubAppConfig;
import io.harness.cistatus.service.GithubService;
import io.harness.cistatus.service.bitbucket.BitbucketConfig;
import io.harness.cistatus.service.bitbucket.BitbucketService;
import io.harness.cistatus.service.gitlab.GitlabConfig;
import io.harness.cistatus.service.gitlab.GitlabService;
import io.harness.cistatus.service.gitlab.GitlabServiceImpl;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ci.status.BuildStatusPushResponse;
import io.harness.delegate.beans.ci.status.BuildStatusPushResponse.Status;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.ci.CIBuildPushParameters.CIBuildPushTaskType;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class CIBuildStatusPushTask extends AbstractDelegateRunnableTask {
  @Inject private GithubService githubService;
  @Inject private BitbucketService bitbucketService;
  @Inject private GitlabService gitlabService;

  private static final String DESC = "description";
  private static final String STATE = "state";
  private static final String URL = "url";
  private static final String CONTEXT = "context";

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

        // TODO encryption details are null because key is not stored in github app config

        boolean statusSent = false;
        // TODO encryption details are null because key is not stored in github app config
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

  private boolean sendBuildStatusToGitHub(CIBuildStatusPushParameters ciBuildStatusPushParameters) {
    GithubAppConfig githubAppConfig = GithubAppConfig.builder()
                                          .installationId(ciBuildStatusPushParameters.getInstallId())
                                          .appId(ciBuildStatusPushParameters.getAppId())
                                          .privateKey(ciBuildStatusPushParameters.getKey())
                                          .githubUrl(GITHUB_API_URL)
                                          .build();
    String token = githubService.getToken(githubAppConfig, null);

    if (isNotEmpty(token)) {
      Map<String, Object> bodyObjectMap = new HashMap<>();
      bodyObjectMap.put(DESC, ciBuildStatusPushParameters.getDesc());
      bodyObjectMap.put(CONTEXT, ciBuildStatusPushParameters.getIdentifier());
      bodyObjectMap.put(STATE, ciBuildStatusPushParameters.getState());

      return githubService.sendStatus(githubAppConfig, token, null, ciBuildStatusPushParameters.getSha(),
          ciBuildStatusPushParameters.getOwner(), ciBuildStatusPushParameters.getRepo(), bodyObjectMap);
    } else {
      log.error("Not sending status because token is empty for appId {}, installationId {}, sha {}",
          ciBuildStatusPushParameters.getInstallId(), ciBuildStatusPushParameters.getInstallId(),
          ciBuildStatusPushParameters.getSha());
      return false;
    }
  }

  private boolean sendBuildStatusToBitbucket(CIBuildStatusPushParameters ciBuildStatusPushParameters) {
    Map<String, Object> bodyObjectMap = new HashMap<>();
    bodyObjectMap.put(DESC, ciBuildStatusPushParameters.getDesc());
    bodyObjectMap.put(BITBUCKET_KEY, ciBuildStatusPushParameters.getIdentifier());
    bodyObjectMap.put(STATE, ciBuildStatusPushParameters.getState());
    bodyObjectMap.put(URL, APP_URL); // Retrieve it via vanity URL

    if (isNotEmpty(ciBuildStatusPushParameters.getToken())) {
      return bitbucketService.sendStatus(BitbucketConfig.builder().bitbucketUrl(BITBUCKET_API_URL).build(),
          ciBuildStatusPushParameters.getUserName(), ciBuildStatusPushParameters.getToken(), null,
          ciBuildStatusPushParameters.getSha(), ciBuildStatusPushParameters.getOwner(),
          ciBuildStatusPushParameters.getRepo(), bodyObjectMap);
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

    if (isNotEmpty(ciBuildStatusPushParameters.getToken())) {
      return gitlabService.sendStatus(GitlabConfig.builder().gitlabUrl(GITLAB_API_URL).build(),
          ciBuildStatusPushParameters.getUserName(), ciBuildStatusPushParameters.getToken(), null,
          ciBuildStatusPushParameters.getSha(), ciBuildStatusPushParameters.getOwner(),
          ciBuildStatusPushParameters.getRepo(), bodyObjectMap);
    } else {
      log.error("Not sending status because token is empty sha {}", ciBuildStatusPushParameters.getSha());
      return false;
    }
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }
}
