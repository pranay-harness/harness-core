package io.harness.artifact;

import com.google.inject.Inject;
import com.google.protobuf.util.Durations;

import io.harness.beans.DelegateTask;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.artifact.ArtifactCollectionTaskParams;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;

import java.util.HashMap;
import java.util.Map;

public class ArtifactCollectionPTaskServiceClient
    implements PerpetualTaskServiceClient<ArtifactCollectionPTaskClientParams> {
  private static final String ARTIFACT_STREAM_ID = "artifactStreamId";
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private ArtifactCollectionUtils artifactCollectionUtils;

  @Override
  public String create(String accountId, ArtifactCollectionPTaskClientParams clientParams) {
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put(ARTIFACT_STREAM_ID, clientParams.getArtifactStreamId());

    PerpetualTaskClientContext clientContext = new PerpetualTaskClientContext(clientParamMap);

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(1))
                                         .setTimeout(Durations.fromSeconds(30))
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
    return ArtifactCollectionTaskParams.newBuilder().setArtifactStreamId(artifactStreamId).build();
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId) {
    Map<String, String> clientParams = clientContext.getClientParams();
    return artifactCollectionUtils.prepareValidateTask(clientParams.get(ARTIFACT_STREAM_ID));
  }
}
