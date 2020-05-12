package io.harness.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Durations;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.exception.InvalidRequestException;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.artifact.ArtifactCollectionTaskParams;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.buildsource.BuildSourceParameters;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;

import java.util.HashMap;
import java.util.Map;

@OwnedBy(CDC)
@Slf4j
public class ArtifactCollectionPTaskServiceClient
    implements PerpetualTaskServiceClient<ArtifactCollectionPTaskClientParams> {
  private static final String ARTIFACT_STREAM_ID = "artifactStreamId";
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private ArtifactCollectionUtils artifactCollectionUtils;

  @Override
  public String create(String accountId, ArtifactCollectionPTaskClientParams clientParams) {
    Map<String, String> clientParamMap = new HashMap<>();
    if (isEmpty(clientParams.getArtifactStreamId())) {
      throw new InvalidRequestException(
          "Failed to create Perpetual Task as Artifact Stream Id is missing from clientParams");
    }
    clientParamMap.put(ARTIFACT_STREAM_ID, clientParams.getArtifactStreamId());

    PerpetualTaskClientContext clientContext = new PerpetualTaskClientContext(clientParamMap);

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(1))
                                         .setTimeout(Durations.fromMinutes(2))
                                         .build();
    return perpetualTaskService.createTask(
        PerpetualTaskType.ARTIFACT_COLLECTION, accountId, clientContext, schedule, false);
  }

  @Override
  public boolean reset(String accountId, String taskId) {
    return perpetualTaskService.resetTask(accountId, taskId);
  }

  @Override
  public boolean delete(String accountId, String taskId) {
    return perpetualTaskService.deleteTask(accountId, taskId);
  }

  @Override
  public ArtifactCollectionTaskParams getTaskParams(PerpetualTaskClientContext clientContext) {
    Map<String, String> clientParams = clientContext.getClientParams();
    String artifactStreamId = clientParams.get(ARTIFACT_STREAM_ID);
    BuildSourceParameters buildSourceParameters =
        artifactCollectionUtils.prepareBuildSourceParameters(artifactStreamId);
    ByteString bytes = ByteString.copyFrom(KryoUtils.asBytes(buildSourceParameters));
    return ArtifactCollectionTaskParams.newBuilder()
        .setArtifactStreamId(artifactStreamId)
        .setBuildSourceParams(bytes)
        .build();
  }

  @Override
  public void onTaskStateChange(
      String taskId, PerpetualTaskResponse newPerpetualTaskResponse, PerpetualTaskResponse oldPerpetualTaskResponse) {
    logger.debug("Nothing to do !!");
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId) {
    Map<String, String> clientParams = clientContext.getClientParams();
    return artifactCollectionUtils.prepareValidateTask(clientParams.get(ARTIFACT_STREAM_ID));
  }
}
