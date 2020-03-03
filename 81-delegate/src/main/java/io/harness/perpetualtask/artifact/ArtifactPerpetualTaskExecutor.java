package io.harness.perpetualtask.artifact;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.grpc.utils.AnyUtils;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskParams;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.buildsource.BuildSourceParameters;

import java.time.Instant;

@Singleton
@Slf4j
public class ArtifactPerpetualTaskExecutor implements PerpetualTaskExecutor {
  private ArtifactRepositoryServiceImpl artifactRepositoryService;

  @Inject
  public ArtifactPerpetualTaskExecutor(ArtifactRepositoryServiceImpl artifactRepositoryService) {
    this.artifactRepositoryService = artifactRepositoryService;
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
    artifactRepositoryService.publishCollectedArtifacts(buildSourceParameters);
    logger.info("Published artifact successfully");
    return true;
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskParams params) {
    return false;
  }
}
