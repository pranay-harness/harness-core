package io.harness.perpetualtask.artifact;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.network.SafeHttpCall.executeWithExceptions;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.grpc.utils.AnyUtils;
import io.harness.managerclient.ManagerClient;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskParams;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import software.wings.delegatetasks.buildsource.BuildSourceExecutionResponse;
import software.wings.delegatetasks.buildsource.BuildSourceParameters;
import software.wings.delegatetasks.buildsource.BuildSourceResponse;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Singleton
@Slf4j
public class ArtifactPerpetualTaskExecutor implements PerpetualTaskExecutor {
  // Deadline until we try to publish parts of build details to the manager. This should be considerably less than than
  // the task timeout. Right now, timeout is 2 minutes, and this value is 1.5 minutes.
  private static final long INTERNAL_TIMEOUT_IN_MS = 90L * 1000;

  private ArtifactRepositoryServiceImpl artifactRepositoryService;
  private ManagerClient managerClient;

  private Cache<String, ArtifactsPublishedCache> cache = Caffeine.newBuilder().build();

  @Inject
  public ArtifactPerpetualTaskExecutor(
      ArtifactRepositoryServiceImpl artifactRepositoryService, ManagerClient managerClient) {
    this.artifactRepositoryService = artifactRepositoryService;
    this.managerClient = managerClient;
  }

  @Override
  public PerpetualTaskResponse runOnce(PerpetualTaskId taskId, PerpetualTaskParams params, Instant heartbeatTime) {
    logger.info("In ArtifactPerpetualTask artifact collection");
    ArtifactCollectionTaskParams artifactCollectionTaskParams = getTaskParams(params);

    String artifactStreamId = artifactCollectionTaskParams.getArtifactStreamId();
    logger.info("Running artifact collection for artifactStreamId: {}", artifactStreamId);

    final BuildSourceParameters buildSourceParameters =
        (BuildSourceParameters) KryoUtils.asObject(artifactCollectionTaskParams.getBuildSourceParams().toByteArray());
    String accountId = buildSourceParameters.getAccountId();

    // Fetch artifacts published cache for this artifactStreamId.
    ArtifactsPublishedCache currCache = getArtifactsPublishedCached(artifactStreamId, buildSourceParameters);
    Instant startTime = Instant.now();
    if (!currCache.needsToPublish()) {
      collectArtifacts(accountId, artifactStreamId, taskId, buildSourceParameters, currCache);
    }

    // This condition is true if we initially had unpublished artifacts, or if artifact collection happened and we got
    // some new unique artifacts after deduplication.
    if (currCache.needsToPublish()) {
      Instant deadline = startTime.plusMillis(INTERNAL_TIMEOUT_IN_MS);
      publishFromCache(accountId, taskId, artifactCollectionTaskParams, currCache, deadline);
    }

    logger.info("Published artifact successfully");
    return PerpetualTaskResponse.builder()
        .responseCode(200)
        .perpetualTaskState(PerpetualTaskState.TASK_RUN_SUCCEEDED)
        .responseMessage(PerpetualTaskState.TASK_RUN_SUCCEEDED.name())
        .build();
  }

  private void collectArtifacts(String accountId, String artifactStreamId, PerpetualTaskId taskId,
      BuildSourceParameters buildSourceParameters, ArtifactsPublishedCache currCache) {
    // Fetch new build details if there are no unpublished build details in the cache.
    final BuildSourceExecutionResponse buildSourceExecutionResponse =
        artifactRepositoryService.publishCollectedArtifacts(buildSourceParameters);

    // Return early if the artifact collection is unsuccessful.
    if (buildSourceExecutionResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      publishToManager(accountId, artifactStreamId, taskId, buildSourceExecutionResponse);
      logger.info("Published unsuccessful artifact collection result");
      return;
    }

    List<BuildDetails> builds = buildSourceExecutionResponse.getBuildSourceResponse().getBuildDetails();
    if (isEmpty(builds)) {
      logger.info("Published empty artifact collection result");
      return;
    }

    // Add to cache and remove duplicates.
    currCache.addArtifactCollectionResult(builds);
  }

  private void publishFromCache(String accountId, PerpetualTaskId taskId, ArtifactCollectionTaskParams taskParams,
      ArtifactsPublishedCache currCache, Instant deadline) {
    // If deadline (which was conservative) has passed, don't try to make any more publish calls to manager.
    if (deadline.isBefore(Instant.now())) {
      return;
    }

    String artifactStreamId = taskParams.getArtifactStreamId();
    if (!publishDeletedArtifactKeys(accountId, artifactStreamId, taskId, currCache)) {
      return;
    }
    if (publishUnpublishedBuildDetails(accountId, artifactStreamId, taskId, currCache)) {
      // Try to republish artifacts until deadline is reached or we have no more unpublished artifacts left.
      publishFromCache(accountId, taskId, taskParams, currCache, deadline);
    }
  }

  /**
   * publishDeletedArtifactKeys publishes artifact keys to the manager that need to be deleted. It returns false if the
   * operation failed unexpectedly.
   */
  private boolean publishDeletedArtifactKeys(
      String accountId, String artifactStreamId, PerpetualTaskId taskId, ArtifactsPublishedCache currCache) {
    Set<String> toBeDeletedArtifactKeys = currCache.getToBeDeletedArtifactKeys();
    if (isEmpty(toBeDeletedArtifactKeys)) {
      logger.info(
          "Empty toBeDeletedArtifactKeys in publishDeletedArtifactKeys for artifactStreamId: {}, perpetualTaskId: {}",
          artifactStreamId, taskId.getId());
      return true;
    }

    BuildSourceExecutionResponse buildSourceExecutionResponse =
        BuildSourceExecutionResponse.builder()
            .buildSourceResponse(
                BuildSourceResponse.builder().toBeDeletedKeys(toBeDeletedArtifactKeys).cleanup(true).build())
            .commandExecutionStatus(CommandExecutionResult.CommandExecutionStatus.SUCCESS)
            .artifactStreamId(artifactStreamId)
            .build();

    boolean published = publishToManager(accountId, artifactStreamId, taskId, buildSourceExecutionResponse);
    if (!published) {
      return false;
    }

    currCache.removeDeletedArtifactKeys(toBeDeletedArtifactKeys);
    return true;
  }

  /**
   * publishUnpublishedBuildDetails publishes build details to the manager that are in the cache and are unpublished. It
   * returns true if the operation succeeds and there are more build details left after the current batch.
   */
  private boolean publishUnpublishedBuildDetails(
      String accountId, String artifactStreamId, PerpetualTaskId taskId, ArtifactsPublishedCache currCache) {
    ImmutablePair<List<BuildDetails>, Boolean> resp = currCache.getLimitedUnpublishedBuildDetails();
    List<BuildDetails> builds = resp.getLeft();
    if (isEmpty(builds)) {
      logger.info("Empty build details in publishUnpublishedBuildDetails for artifactStreamId: {}, perpetualTaskId: {}",
          artifactStreamId, taskId.getId());
      return false;
    }

    // This signifies there are more build details left after this batch.
    boolean hasMoreToPublish = resp.getRight();
    BuildSourceExecutionResponse buildSourceExecutionResponse =
        BuildSourceExecutionResponse.builder()
            .buildSourceResponse(BuildSourceResponse.builder().buildDetails(builds).stable(!hasMoreToPublish).build())
            .commandExecutionStatus(CommandExecutionResult.CommandExecutionStatus.SUCCESS)
            .artifactStreamId(artifactStreamId)
            .build();

    boolean published = publishToManager(accountId, artifactStreamId, taskId, buildSourceExecutionResponse);
    if (!published) {
      return false;
    }

    currCache.addPublishedBuildDetails(builds);
    return hasMoreToPublish;
  }

  private boolean publishToManager(String accountId, String artifactStreamId, PerpetualTaskId taskId,
      BuildSourceExecutionResponse buildSourceExecutionResponse) {
    try {
      executeWithExceptions(
          managerClient.publishArtifactCollectionResult(taskId.getId(), accountId, buildSourceExecutionResponse));
      return true;
    } catch (Exception ex) {
      logger.error(
          format(
              "Failed to publish build source execution response with status: %s for artifactStreamId: %s, perpetualTaskId: %s",
              artifactStreamId, buildSourceExecutionResponse.getCommandExecutionStatus().name(), taskId.getId()),
          ex);
      return false;
    }
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskParams params) {
    ArtifactCollectionTaskParams taskParams = getTaskParams(params);
    cache.invalidate(taskParams.getArtifactStreamId());
    return false;
  }

  private ArtifactCollectionTaskParams getTaskParams(PerpetualTaskParams params) {
    return AnyUtils.unpack(params.getCustomizedParams(), ArtifactCollectionTaskParams.class);
  }

  private ArtifactsPublishedCache getArtifactsPublishedCached(
      String artifactStreamId, BuildSourceParameters buildSourceParameters) {
    Function<BuildDetails, String> buildDetailsKeyFn = ArtifactCollectionUtils.getBuildDetailsKeyFn(
        buildSourceParameters.getArtifactStreamType(), buildSourceParameters.getArtifactStreamAttributes());
    boolean enableCleanup = ArtifactCollectionUtils.supportsCleanup(buildSourceParameters.getArtifactStreamType());
    return cache.get(artifactStreamId,
        id
        -> new ArtifactsPublishedCache(
            buildSourceParameters.getSavedBuildDetailsKeys(), buildDetailsKeyFn, enableCleanup));
  }
}
