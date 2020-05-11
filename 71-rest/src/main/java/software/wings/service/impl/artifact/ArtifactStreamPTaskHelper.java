package software.wings.service.impl.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifact.ArtifactCollectionPTaskClientParams;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.AutoLogContext;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskServiceClientRegistry;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.persistence.AccountLogContext;
import io.harness.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.delegatetasks.buildsource.ArtifactStreamLogContext;
import software.wings.service.intfc.ArtifactStreamService;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class ArtifactStreamPTaskHelper {
  @Inject private PerpetualTaskServiceClientRegistry clientRegistry;
  @Inject private ArtifactStreamService artifactStreamService;

  private PerpetualTaskServiceClient<ArtifactCollectionPTaskClientParams> getClient() {
    return (PerpetualTaskServiceClient<ArtifactCollectionPTaskClientParams>) clientRegistry.getClient(
        PerpetualTaskType.ARTIFACT_COLLECTION);
  }

  public boolean reset(String accountId, String taskId) {
    return getClient().reset(accountId, taskId);
  }

  public boolean delete(String accountId, String taskId) {
    return getClient().delete(accountId, taskId);
  }

  public void createPerpetualTask(ArtifactStream artifactStream) {
    Validator.notNullCheck("ArtifactStreamId is missing", artifactStream.getUuid());
    if (artifactStream.getPerpetualTaskId() != null) {
      throw new InvalidRequestException(
          format("Perpetual task already exists for artifact stream [%s]", artifactStream.getUuid()));
    }

    try (AutoLogContext ignore1 = new AccountLogContext(artifactStream.getAccountId(), OVERRIDE_ERROR);
         AutoLogContext ignore2 = new ArtifactStreamLogContext(artifactStream.getUuid(), OVERRIDE_ERROR)) {
      PerpetualTaskServiceClient<ArtifactCollectionPTaskClientParams> client = getClient();
      ArtifactCollectionPTaskClientParams artifactCollectionPTaskClientParams =
          ArtifactCollectionPTaskClientParams.builder().artifactStreamId(artifactStream.getUuid()).build();
      logger.info("Creating perpetual task");

      String perpetualTaskId = client.create(artifactStream.getAccountId(), artifactCollectionPTaskClientParams);
      logger.info("Created perpetual task: {}", perpetualTaskId);

      boolean updated = artifactStreamService.attachPerpetualTaskId(artifactStream, perpetualTaskId);
      if (!updated) {
        // If artifact stream is not updated, it doesn't know about the perpetual task. So the perpetual task becomes a
        // zombie and is never reset or deleted on config change. It might try to do regular collection along with
        // perpetual task which can lead to race conditions.
        client.delete(artifactStream.getAccountId(), perpetualTaskId);
      }
    } catch (Exception ex) {
      // This is background-type operation. Artifact stream can be created but perpetual task creation can fail. We
      // should not fail the save operation and try assigning to perpetual task later.
      logger.error(format("Unable to create perpetual task for artifactStreamId: %s", artifactStream.getUuid()), ex);
    }
  }
}
