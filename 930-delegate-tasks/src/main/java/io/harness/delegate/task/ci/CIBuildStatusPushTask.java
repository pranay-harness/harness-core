package io.harness.delegate.task.ci;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cistatus.service.GithubAppConfig;
import io.harness.cistatus.service.GithubService;
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

  private static final String DESC = "description";
  private static final String STATE = "state";
  private static final String CONTEXT = "context";
  private static final String GITHUB_API_URL = "https://api.github.com/";

  public CIBuildStatusPushTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    if (((CIBuildPushParameters) parameters).commandType == CIBuildPushTaskType.STATUS) {
      try {
        CIBuildStatusPushParameters ciBuildStatusPushParameters = (CIBuildStatusPushParameters) parameters;
        GithubAppConfig githubAppConfig = GithubAppConfig.builder()
                                              .installationId(ciBuildStatusPushParameters.getInstallId())
                                              .appId(ciBuildStatusPushParameters.getAppId())
                                              .privateKey(ciBuildStatusPushParameters.getKey())
                                              .githubUrl(GITHUB_API_URL)
                                              .build();

        // TODO encryption details are null because key is not stored in github app config

        String token = githubService.getToken(githubAppConfig, null);

        if (isNotEmpty(token)) {
          Map<String, Object> bodyObjectMap = new HashMap<>();
          bodyObjectMap.put(DESC, ciBuildStatusPushParameters.getDesc());
          bodyObjectMap.put(CONTEXT, ciBuildStatusPushParameters.getIdentifier());
          bodyObjectMap.put(STATE, ciBuildStatusPushParameters.getState());

          // TODO encryption details are null because key is not stored in github app config
          boolean status = githubService.sendStatus(githubAppConfig, token, null, ciBuildStatusPushParameters.getSha(),
              ciBuildStatusPushParameters.getOwner(), ciBuildStatusPushParameters.getRepo(), bodyObjectMap);

          if (status == true) {
            return BuildStatusPushResponse.builder().status(Status.SUCCESS).build();
          } else {
            return BuildStatusPushResponse.builder().status(Status.ERROR).build();
          }
        }
      } catch (Exception ex) {
        log.error("failed to send status", ex);
        return BuildStatusPushResponse.builder().status(Status.ERROR).build();
      }
    }
    return BuildStatusPushResponse.builder().status(Status.ERROR).build();
  }
  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }
}
