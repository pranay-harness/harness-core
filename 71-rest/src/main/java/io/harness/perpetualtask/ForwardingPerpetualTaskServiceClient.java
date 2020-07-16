package io.harness.perpetualtask;

import static com.google.common.base.Strings.nullToEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;
import com.google.protobuf.Message;
import com.google.protobuf.util.Durations;

import io.harness.NgManagerServiceDriver;
import io.harness.beans.DelegateTask;
import io.harness.delegate.NgTaskDetails;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.TaskParameters;
import io.harness.grpc.utils.AnyUtils;
import io.harness.grpc.utils.HTimestamps;
import io.harness.serializer.KryoSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;

import java.util.Map;

@Slf4j
public class ForwardingPerpetualTaskServiceClient implements PerpetualTaskServiceClient {
  @Inject private NgManagerServiceDriver ngManagerServiceDriver;
  @Inject private KryoSerializer kryoSerializer;
  private final String perpetualTaskType;
  private final String remoteServiceId;

  public ForwardingPerpetualTaskServiceClient(String perpetualTaskType, String remoteServiceId) {
    this.perpetualTaskType = perpetualTaskType;
    this.remoteServiceId = remoteServiceId;
  }

  @Override
  public Message getTaskParams(PerpetualTaskClientContext clientContext) {
    final ObtainPerpetualTaskExecutionParamsResponse response =
        getDriverForService(remoteServiceId)
            .obtainPerpetualTaskExecutionParams(
                ObtainPerpetualTaskExecutionParamsRequest.newBuilder()
                    .setTaskType(perpetualTaskType)
                    .setContext(
                        RemotePerpetualTaskClientContext.newBuilder()
                            .setLastContextUpdated(HTimestamps.fromMillis(clientContext.getLastContextUpdated()))
                            .putAllTaskClientParams(MapUtils.emptyIfNull(clientContext.getClientParams()))
                            .build())

                    .build());
    return AnyUtils.findClassAndUnpack(response.getCustomizedParams());
  }

  @Override
  public void onTaskStateChange(
      String taskId, PerpetualTaskResponse newPerpetualTaskResponse, PerpetualTaskResponse oldPerpetualTaskResponse) {
    getDriverForService(remoteServiceId)
        .reportPerpetualTaskStateChange(
            ReportPerpetualTaskStateChangeRequest.newBuilder()
                .setTaskType(perpetualTaskType)
                .setPerpetualTaskId(taskId)
                .setNewTaskResponse(PerpetualTaskExecutionResponse.newBuilder()
                                        .setTaskState(newPerpetualTaskResponse.getPerpetualTaskState().name())
                                        .setResponseCode(newPerpetualTaskResponse.getResponseCode())
                                        .setResponseMessage(nullToEmpty(newPerpetualTaskResponse.getResponseMessage()))
                                        .build())
                .setOldTaskResponse(PerpetualTaskExecutionResponse.newBuilder()
                                        .setResponseMessage(nullToEmpty(oldPerpetualTaskResponse.getResponseMessage()))
                                        .setResponseCode(oldPerpetualTaskResponse.getResponseCode())
                                        .setTaskState(oldPerpetualTaskResponse.getPerpetualTaskState().name())
                                        .build())
                .build());
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId) {
    final ObtainPerpetualTaskValidationDetailsResponse response =
        getDriverForService(remoteServiceId)
            .obtainPerpetualTaskValidationDetails(
                ObtainPerpetualTaskValidationDetailsRequest.newBuilder()
                    .setAccountId(accountId)
                    .setTaskType(perpetualTaskType)
                    .setContext(
                        RemotePerpetualTaskClientContext.newBuilder()
                            .setLastContextUpdated(HTimestamps.fromMillis(clientContext.getLastContextUpdated()))
                            .putAllTaskClientParams(MapUtils.emptyIfNull(clientContext.getClientParams()))
                            .build())
                    .build());

    return extractDelegateTask(response, accountId);
  }

  private DelegateTask extractDelegateTask(ObtainPerpetualTaskValidationDetailsResponse response, String accountId) {
    NgTaskDetails taskDetails = response.getDetails();
    Map<String, String> setupAbstractions = response.getSetupAbstractions().getValuesMap();
    return extractDelegateTask(accountId, setupAbstractions, taskDetails);
  }

  private NgManagerServiceDriver getDriverForService(String remoteServiceId) {
    // TODO @rk: 08/07/20 : find client for the necessary microservice
    return ngManagerServiceDriver;
  }

  private DelegateTask extractDelegateTask(
      String accountId, Map<String, String> setupAbstractions, NgTaskDetails taskDetails) {
    TaskParameters parameters =
        (TaskParameters) kryoSerializer.asInflatedObject(taskDetails.getKryoParameters().toByteArray());
    String taskId = generateUuid();
    return DelegateTask.builder()
        .uuid(taskId)
        .waitId(taskId)
        .setupAbstractions(setupAbstractions)
        .accountId(accountId)
        .data(TaskData.builder()
                  .taskType(taskDetails.getType().getType())
                  .timeout(Durations.toMillis(taskDetails.getExecutionTimeout()))
                  .parameters(new Object[] {parameters})
                  .expressions(taskDetails.getExpressionsMap())
                  .expressionFunctorToken((int) taskDetails.getExpressionFunctorToken())
                  .build())
        .build();
  }
}
