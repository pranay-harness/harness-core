package io.harness.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.perpetualtask.PerpetualTaskLogContext;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.persistence.AccountLogContext;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.FeatureName;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.ArtifactCollectionFailedAlert;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.buildsource.ArtifactStreamLogContext;
import software.wings.delegatetasks.buildsource.BuildSourceExecutionResponse;
import software.wings.delegatetasks.buildsource.BuildSourceResponse;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.TriggerService;

import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class ArtifactCollectionResponseHandler {
  private static final int MAX_ARTIFACTS_COLLECTION_FOR_WARN = 100;
  private static final int MAX_LOGS = 100;
  public static final int MAX_FAILED_ATTEMPTS = 3500;

  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ArtifactCollectionUtils artifactCollectionUtils;
  @Inject private ArtifactService artifactService;
  @Inject private TriggerService triggerService;
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private AlertService alertService;
  @Inject private FeatureFlagService featureFlagService;

  public void processArtifactCollectionResult(@NotNull String accountId, @NotNull String perpetualTaskId,
      @NotNull BuildSourceExecutionResponse buildSourceExecutionResponse) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new PerpetualTaskLogContext(perpetualTaskId, OVERRIDE_ERROR)) {
      ArtifactStream artifactStream = artifactStreamService.get(buildSourceExecutionResponse.getArtifactStreamId());
      if (artifactStream == null) {
        perpetualTaskService.deleteTask(accountId, perpetualTaskId);
        return;
      }

      try (AutoLogContext ignore3 = new ArtifactStreamLogContext(
               artifactStream.getUuid(), artifactStream.getArtifactStreamType(), OVERRIDE_ERROR)) {
        if (!perpetualTaskId.equals(artifactStream.getPerpetualTaskId())
            || !featureFlagService.isEnabled(FeatureName.ARTIFACT_PERPETUAL_TASK, artifactStream.getAccountId())) {
          return;
        }

        if (buildSourceExecutionResponse.getCommandExecutionStatus() != SUCCESS) {
          onFailure(perpetualTaskId, artifactStream);
          return;
        }

        try {
          handleResponseInternal(artifactStream, buildSourceExecutionResponse);
          onSuccess(artifactStream);
        } catch (Exception ex) {
          logger.error("Error while processing artifact collection", ex);
        }
      }
    }
  }

  @VisibleForTesting
  void handleResponseInternal(
      ArtifactStream artifactStream, BuildSourceExecutionResponse buildSourceExecutionResponse) {
    // NOTE: buildSourceResponse is not null at this point.
    BuildSourceResponse buildSourceResponse = buildSourceExecutionResponse.getBuildSourceResponse();
    if (buildSourceResponse.isCleanup()) {
      handleCleanup(artifactStream, buildSourceResponse);
    } else {
      handleCollection(artifactStream, buildSourceResponse);
    }
  }

  private void handleCollection(ArtifactStream artifactStream, BuildSourceResponse buildSourceResponse) {
    List<BuildDetails> builds = buildSourceResponse.getBuildDetails();
    List<Artifact> artifacts = artifactCollectionUtils.processBuilds(artifactStream, builds);
    if (isEmpty(artifacts)) {
      return;
    }

    if (artifacts.size() > MAX_ARTIFACTS_COLLECTION_FOR_WARN) {
      logger.warn("Collected {} artifacts in single collection", artifacts.size());
    }

    artifacts.stream().limit(MAX_LOGS).forEach(
        artifact -> logger.info("New build number [{}] collected", artifact.getBuildNo()));

    if (buildSourceResponse.isStable()) {
      triggerService.triggerExecutionPostArtifactCollectionAsync(
          artifactStream.getAccountId(), artifactStream.fetchAppId(), artifactStream.getUuid(), artifacts);
    }
  }

  private void handleCleanup(ArtifactStream artifactStream, BuildSourceResponse buildSourceResponse) {
    String artifactStreamType = artifactStream.getArtifactStreamType();
    if (!ArtifactCollectionUtils.supportsCleanup(artifactStreamType)) {
      return;
    }

    logger.info("Artifact cleanup started");
    ArtifactStreamAttributes artifactStreamAttributes =
        artifactCollectionUtils.getArtifactStreamAttributes(artifactStream,
            featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, artifactStream.getAccountId()));

    Set<String> artifactKeys = buildSourceResponse.getToBeDeletedKeys();
    boolean deleted =
        artifactService.deleteArtifactsByUniqueKey(artifactStream, artifactStreamAttributes, artifactKeys);
    logger.info("Artifact cleanup completed: deleted = {}, count = {}", deleted, artifactKeys.size());
  }

  private void onSuccess(ArtifactStream artifactStream) {
    if (artifactStream.getFailedCronAttempts() == 0) {
      return;
    }

    logger.info("Successfully fetched builds after {} failures", artifactStream.getFailedCronAttempts());
    artifactStreamService.updateFailedCronAttempts(artifactStream.getAccountId(), artifactStream.getUuid(), 0);
    alertService.closeAlert(artifactStream.getAccountId(), null, AlertType.ARTIFACT_COLLECTION_FAILED,
        ArtifactCollectionFailedAlert.builder().artifactStreamId(artifactStream.getUuid()).build());
  }

  private void onFailure(String perpetualTaskId, ArtifactStream artifactStream) {
    int failedCronAttempts = artifactStream.getFailedCronAttempts() + 1;
    if (failedCronAttempts % 25 == 0) {
      perpetualTaskService.resetTask(artifactStream.getAccountId(), perpetualTaskId);
    }

    artifactStreamService.updateFailedCronAttempts(
        artifactStream.getAccountId(), artifactStream.getUuid(), failedCronAttempts);
    logger.warn("Failed to fetch/process builds, total failed attempts: {}", failedCronAttempts);
    if (failedCronAttempts != MAX_FAILED_ATTEMPTS) {
      return;
    }

    String appId = artifactStream.fetchAppId();
    ArtifactCollectionFailedAlert artifactCollectionFailedAlert;
    if (!GLOBAL_APP_ID.equals(appId)) {
      artifactCollectionFailedAlert = ArtifactCollectionFailedAlert.builder()
                                          .appId(appId)
                                          .serviceId(artifactStream.getServiceId())
                                          .artifactStreamId(artifactStream.getUuid())
                                          .build();
    } else {
      artifactCollectionFailedAlert = ArtifactCollectionFailedAlert.builder()
                                          .settingId(artifactStream.getSettingId())
                                          .artifactStreamId(artifactStream.getUuid())
                                          .build();
    }

    alertService.openAlert(
        artifactStream.getAccountId(), null, AlertType.ARTIFACT_COLLECTION_FAILED, artifactCollectionFailedAlert);

    if (!perpetualTaskService.deleteTask(artifactStream.getAccountId(), artifactStream.getPerpetualTaskId())) {
      logger.error(String.format(
          "Unable to delete artifact collection perpetual task: %s", artifactStream.getPerpetualTaskId()));
    }
    artifactStream.setPerpetualTaskId(null);
    artifactStreamService.update(artifactStream);
  }
}
