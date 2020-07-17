package software.wings.service.impl.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.util.Durations;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifact.ArtifactCollectionPTaskClientParams;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.delegatetasks.buildsource.ArtifactStreamLogContext;
import software.wings.service.intfc.ArtifactStreamService;

import java.util.HashMap;
import java.util.Map;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class ArtifactStreamPTaskHelper {
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private PerpetualTaskService perpetualTaskService;

  private static final String ARTIFACT_STREAM_ID = "artifactStreamId";

  public void createPerpetualTask(ArtifactStream artifactStream) {
    Validator.notNullCheck("Artifact stream id is missing", artifactStream.getUuid());
    if (artifactStream.getPerpetualTaskId() != null) {
      throw new InvalidRequestException(
          format("Perpetual task already exists for artifact stream: %s", artifactStream.getUuid()));
    }

    try (AutoLogContext ignore1 = new AccountLogContext(artifactStream.getAccountId(), OVERRIDE_ERROR);
         AutoLogContext ignore2 = new ArtifactStreamLogContext(artifactStream.getUuid(), OVERRIDE_ERROR)) {
      ArtifactCollectionPTaskClientParams artifactCollectionPTaskClientParams =
          ArtifactCollectionPTaskClientParams.builder().artifactStreamId(artifactStream.getUuid()).build();
      logger.info("Creating perpetual task");

      String perpetualTaskId = create(artifactStream.getAccountId(), artifactCollectionPTaskClientParams);
      logger.info("Created perpetual task: {}", perpetualTaskId);

      boolean updated = false;
      try {
        updated = artifactStreamService.attachPerpetualTaskId(artifactStream, perpetualTaskId);
        logger.info("Attaching perpetual task: {} to artifact stream: {}", perpetualTaskId, artifactStream.getUuid());
      } finally {
        if (!updated) {
          // If artifact stream is not updated, it doesn't know about the perpetual task. So the perpetual task becomes
          // a zombie and is never reset or deleted on config change. It might try to do regular collection along with
          // perpetual task which can lead to race conditions.
          perpetualTaskService.deleteTask(artifactStream.getAccountId(), perpetualTaskId);
        }
      }
    } catch (Exception ex) {
      // This is background-type operation. Artifact stream can be created but perpetual task creation can fail. We
      // should not fail the save operation and try assigning to perpetual task later.
      logger.error(format("Unable to create perpetual task for artifact stream: %s", artifactStream.getUuid()), ex);
    }
  }

  private String create(String accountId, ArtifactCollectionPTaskClientParams clientParams) {
    Map<String, String> clientParamMap = new HashMap<>();
    if (isEmpty(clientParams.getArtifactStreamId())) {
      throw new InvalidRequestException(
          "Failed to create Perpetual Task as Artifact Stream Id is missing from clientParams");
    }
    clientParamMap.put(ARTIFACT_STREAM_ID, clientParams.getArtifactStreamId());

    PerpetualTaskClientContext clientContext =
        PerpetualTaskClientContext.builder().clientParams(clientParamMap).build();

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(1))
                                         .setTimeout(Durations.fromMinutes(2))
                                         .build();
    return perpetualTaskService.createTask(
        PerpetualTaskType.ARTIFACT_COLLECTION, accountId, clientContext, schedule, false);
  }
}
