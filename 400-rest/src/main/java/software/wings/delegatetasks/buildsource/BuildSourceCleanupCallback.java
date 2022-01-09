/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.buildsource;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.tasks.ResponseData;
import io.harness.waiter.OldNotifyCallback;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.ArtifactStreamService;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@OwnedBy(CDC)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class BuildSourceCleanupCallback implements OldNotifyCallback {
  private String accountId;
  private String artifactStreamId;
  private List<BuildDetails> builds;

  @Inject private transient ArtifactStreamService artifactStreamService;
  @Inject @Named("BuildSourceCleanupCallbackExecutor") private ExecutorService executorService;
  @Inject private BuildSourceCleanupHelper buildSourceCleanupHelper;

  public BuildSourceCleanupCallback() {}

  public BuildSourceCleanupCallback(String accountId, String artifactStreamId) {
    this.accountId = accountId;
    this.artifactStreamId = artifactStreamId;
  }

  @VisibleForTesting
  void handleResponseForSuccessInternal(DelegateResponseData notifyResponseData, ArtifactStream artifactStream) {
    log.info("Processing response for BuildSourceCleanupCallback for accountId:[{}] artifactStreamId:[{}]", accountId,
        artifactStreamId);

    BuildSourceExecutionResponse buildSourceExecutionResponse = (BuildSourceExecutionResponse) notifyResponseData;
    if (buildSourceExecutionResponse.getBuildSourceResponse() != null) {
      builds = buildSourceExecutionResponse.getBuildSourceResponse().getBuildDetails();
    } else {
      log.warn(
          "ASYNC_ARTIFACT_CLEANUP: null BuildSourceResponse in buildSourceExecutionResponse:[{}] for artifactStreamId [{}]",
          buildSourceExecutionResponse, artifactStreamId);
    }
    buildSourceCleanupHelper.cleanupArtifacts(accountId, artifactStream, builds);
  }

  private void handleResponseForSuccess(DelegateResponseData notifyResponseData, ArtifactStream artifactStream) {
    try {
      executorService.submit(() -> {
        try {
          handleResponseForSuccessInternal(notifyResponseData, artifactStream);
        } catch (Exception ex) {
          log.error(
              "Error while processing response for BuildSourceCleanupCallback for accountId:[{}] artifactStreamId:[{}]",
              accountId, artifactStreamId, ex);
        }
      });
    } catch (RejectedExecutionException ex) {
      log.error("RejectedExecutionException for BuildSourceCleanupCallback for accountId:[{}] artifactStreamId:[{}]",
          accountId, artifactStreamId, ex);
    }
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    DelegateResponseData notifyResponseData = (DelegateResponseData) response.values().iterator().next();
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
    if (notifyResponseData instanceof BuildSourceExecutionResponse) {
      if (SUCCESS == ((BuildSourceExecutionResponse) notifyResponseData).getCommandExecutionStatus()) {
        handleResponseForSuccess(notifyResponseData, artifactStream);
      } else {
        log.info("Request failed :[{}]", ((BuildSourceExecutionResponse) notifyResponseData).getErrorMessage());
      }
    } else {
      notifyError(response);
    }
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    DelegateResponseData notifyResponseData = (DelegateResponseData) response.values().iterator().next();
    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      log.info("Request failed :[{}]", ((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
    } else {
      log.error("Unexpected  notify response:[{}] during artifact collection for artifactStreamId {} ", response,
          artifactStreamId);
    }
  }
}
