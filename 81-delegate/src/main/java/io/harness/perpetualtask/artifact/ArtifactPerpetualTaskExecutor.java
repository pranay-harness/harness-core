package io.harness.perpetualtask.artifact;

import static io.harness.network.SafeHttpCall.execute;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.grpc.utils.AnyUtils;
import io.harness.managerclient.ManagerClientV2;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskParams;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.buildsource.BuildSourceExecutionResponse;
import software.wings.delegatetasks.buildsource.BuildSourceParameters;

import java.time.Instant;

@Singleton
@Slf4j
public class ArtifactPerpetualTaskExecutor implements PerpetualTaskExecutor {
  private ArtifactRepositoryServiceImpl artifactRepositoryService;
  private ManagerClientV2 managerClient;
  @Inject
  public ArtifactPerpetualTaskExecutor(
      ArtifactRepositoryServiceImpl artifactRepositoryService, ManagerClientV2 managerClient) {
    this.artifactRepositoryService = artifactRepositoryService;
    this.managerClient = managerClient;
  }

  @Override
  public boolean runOnce(PerpetualTaskId taskId, PerpetualTaskParams params, Instant heartbeatTime) {
    logger.info("In ArtifactPerpetualTask artifact collection");
    ArtifactCollectionTaskParams artifactCollectionTaskParams =
        AnyUtils.unpack(params.getCustomizedParams(), ArtifactCollectionTaskParams.class);
    logger.info("Running artifact collection for artifactStreamId", artifactCollectionTaskParams.getArtifactStreamId());
    final BuildSourceParameters buildSourceParameters =
        (BuildSourceParameters) KryoUtils.asObject(artifactCollectionTaskParams.getBuildSourceParams().toByteArray());
    // Fetch the Builds
    final BuildSourceExecutionResponse buildSourceExecutionResponse =
        artifactRepositoryService.publishCollectedArtifacts(buildSourceParameters);
    // Publish the data
    try {
      execute(managerClient.publishArtifactCollectionResult(
          taskId.getId(), buildSourceParameters.getAccountId(), buildSourceExecutionResponse));
    } catch (Exception ex) {
      logger.error(
          "Failed to publish the artifact collection result to manager for artifactStreamId {} and PerpetualTaskId {}",
          taskId.getId(), ex);
    }
    logger.info("Published artifact successfully");
    return true;
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskParams params) {
    return false;
  }
}
