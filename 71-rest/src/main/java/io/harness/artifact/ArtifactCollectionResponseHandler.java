package io.harness.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.FeatureName;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.buildsource.ArtifactStreamLogContext;
import software.wings.delegatetasks.buildsource.BuildSourceExecutionResponse;
import software.wings.delegatetasks.buildsource.BuildSourceResponse;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.TriggerService;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class ArtifactCollectionResponseHandler {
  private static final int MAX_ARTIFACTS_COLLECTION_FOR_WARN = 100;
  private static final int MAX_LOGS = 2000;

  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ArtifactCollectionUtils artifactCollectionUtils;
  @Inject private ArtifactService artifactService;
  @Inject private TriggerService triggerService;
  @Inject private FeatureFlagService featureFlagService;

  @Inject @Named("BuildSourceCallbackExecutor") private ExecutorService executorService;

  public void processArtifactCollectionResult(BuildSourceExecutionResponse buildSourceExecutionResponse) {
    if (buildSourceExecutionResponse.getCommandExecutionStatus() != SUCCESS) {
      return;
    }

    ArtifactStream artifactStream = artifactStreamService.get(buildSourceExecutionResponse.getArtifactStreamId());
    if (artifactStream == null) {
      return; // Possibly clean up the perpetual task.
    }

    try (AutoLogContext ignore1 = new AccountLogContext(artifactStream.getAccountId(), OVERRIDE_ERROR);
         AutoLogContext ignore2 = new ArtifactStreamLogContext(
             artifactStream.getUuid(), artifactStream.getArtifactStreamType(), OVERRIDE_ERROR)) {
      try {
        executorService.submit(() -> {
          try {
            handleResponseInternal(artifactStream, buildSourceExecutionResponse);
          } catch (Exception ex) {
            logger.error("Error while processing artifact collection", ex);
          }
        });
      } catch (RejectedExecutionException ex) {
        logger.error("RejectedExecutionException for BuildSourceCallback");
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

    try (AutoLogContext ignore1 = new AccountLogContext(artifactStream.getAccountId(), OVERRIDE_ERROR);
         AutoLogContext ignore2 = new ArtifactStreamLogContext(
             artifactStream.getUuid(), artifactStream.getArtifactStreamType(), OVERRIDE_ERROR)) {
      if (artifacts.size() > MAX_ARTIFACTS_COLLECTION_FOR_WARN) {
        logger.warn("Collected {} artifacts in single collection", artifacts.size());
      }

      artifacts.stream().limit(MAX_LOGS).forEach(
          artifact -> logger.info("New build number [{}] collected", artifact.getBuildNo()));
    }

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

    String artifactStreamId = artifactStream.getUuid();
    logger.info("Artifact stream [{}] cleanup started with type: {}, name: {}", artifactStreamId,
        artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
    ArtifactStreamAttributes artifactStreamAttributes =
        artifactCollectionUtils.getArtifactStreamAttributes(artifactStream,
            featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, artifactStream.getAccountId()));

    Set<String> artifactKeys = buildSourceResponse.getToBeDeletedKeys();
    boolean deleted =
        artifactService.deleteArtifactsByUniqueKey(artifactStream, artifactStreamAttributes, artifactKeys);
    logger.info("Artifact stream [{}] cleanup completed [deleted={}] with type: {}, count: {}", artifactStreamId,
        deleted, artifactStream.getArtifactStreamType(), artifactKeys.size());
  }
}
