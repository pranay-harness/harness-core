package io.harness.artifact;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static software.wings.beans.artifact.ArtifactStreamCollectionStatus.STABLE;
import static software.wings.beans.artifact.ArtifactStreamCollectionStatus.UNSTABLE;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.delegatetasks.buildsource.ArtifactStreamLogContext;
import software.wings.delegatetasks.buildsource.BuildSourceExecutionResponse;
import software.wings.delegatetasks.buildsource.BuildSourceResponse;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.TriggerService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Singleton
public class ArtifactCollectionResponseHandler {
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ArtifactCollectionUtils artifactCollectionUtils;
  @Inject private TriggerService triggerService;
  private static final int MAX_ARTIFACTS_COLLECTION_FOR_WARN = 100;
  private static final int MAX_LOGS = 2000;

  @Inject @Named("BuildSourceCallbackExecutor") private ExecutorService executorService;

  public void processArtifactCollectionResult(BuildSourceExecutionResponse buildSourceExecutionResponse) {
    if (SUCCESS == buildSourceExecutionResponse.getCommandExecutionStatus()) {
      try {
        executorService.submit(() -> {
          try {
            handleResponseInternal(buildSourceExecutionResponse);
          } catch (Exception ex) {
            logger.error("Error while processing artifact collection for artifactStreamId {} ",
                buildSourceExecutionResponse.getArtifactStreamId(), ex);
          }
        });
      } catch (RejectedExecutionException ex) {
        logger.error("RejectedExecutionException for BuildSourceCallback for artifactStreamId {}",
            buildSourceExecutionResponse.getArtifactStreamId());
      }
    }
  }

  @VisibleForTesting
  void handleResponseInternal(BuildSourceExecutionResponse buildSourceExecutionResponse) {
    BuildSourceResponse buildSourceResponse = buildSourceExecutionResponse.getBuildSourceResponse();
    ArtifactStream artifactStream = artifactStreamService.get(buildSourceExecutionResponse.getArtifactStreamId());
    List<BuildDetails> builds = buildSourceExecutionResponse.getBuildSourceResponse().getBuildDetails();
    // NOTE: buildSourceResponse is not null at this point.

    boolean isUnstable = UNSTABLE.name().equals(artifactStream.getCollectionStatus());
    if (isUnstable && buildSourceResponse.isStable()) {
      // If the artifact stream is unstable and buildSourceResponse has stable as true, mark this artifact
      // stream as stable.
      artifactStreamService.updateCollectionStatus(
          artifactStream.getAccountId(), artifactStream.getUuid(), STABLE.name());
    }
    List<Artifact> artifacts = artifactCollectionUtils.processBuilds(artifactStream, builds);

    if (artifacts.size() > MAX_ARTIFACTS_COLLECTION_FOR_WARN) {
      logger.warn("Collected {} artifacts in single collection artifactStreamId:[{}]", artifacts.size(),
          artifactStream.getUuid());
    }
    if (isNotEmpty(artifacts)) {
      artifacts.stream().limit(MAX_LOGS).forEach(artifact -> {
        try (AutoLogContext ignore1 = new AccountLogContext(artifactStream.getAccountId(), OVERRIDE_ERROR);
             AutoLogContext ignore2 = new ArtifactStreamLogContext(
                 artifactStream.getUuid(), artifactStream.getArtifactStreamType(), OVERRIDE_ERROR)) {
          logger.info("Build number {} new artifacts collected artifactStreamId:[{}]", artifact.getBuildNo(),
              artifactStream.getUuid());
        }
      });

      if (!isUnstable) {
        triggerService.triggerExecutionPostArtifactCollectionAsync(
            artifactStream.getAccountId(), artifactStream.fetchAppId(), artifactStream.getUuid(), artifacts);
      }
    }
  }
}
