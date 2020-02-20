package software.wings.service.impl.artifact;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.artifact.ArtifactCollectionPTaskClientParams;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskServiceClientRegistry;
import io.harness.perpetualtask.PerpetualTaskType;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.artifact.ArtifactStreamServiceObserver;

@Slf4j
@Singleton
public class ArtifactStreamPerpetualTaskManager implements ArtifactStreamServiceObserver {
  @Inject private PerpetualTaskServiceClientRegistry clientRegistry;
  @Inject private ArtifactStreamService artifactStreamService;

  @Override
  public void onSaved(ArtifactStream artifactStream) {
    PerpetualTaskServiceClient client = clientRegistry.getClient(PerpetualTaskType.ARTIFACT_COLLECTION);
    ArtifactCollectionPTaskClientParams artifactCollectionPTaskClientParams =
        ArtifactCollectionPTaskClientParams.builder().artifactStreamId(artifactStream.getArtifactStreamId()).build();
    logger.info("Creating Perpetual Task for artifactStreamId [{}] of accountId [{}]", artifactStream.getUuid(),
        artifactStream.getAccountId());
    String watcherTaskId = client.create(artifactStream.getAccountId(), artifactCollectionPTaskClientParams);
    logger.info("Perpetual Task for artifactStreamId [{}] of accountId [{}] success with watcherTaskId [{}]",
        artifactStream.getUuid(), artifactStream.getAccountId(), watcherTaskId);
    artifactStreamService.attachPerpetualTaskId(artifactStream, watcherTaskId);
  }
}
