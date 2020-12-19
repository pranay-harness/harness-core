package io.harness.pms.sdk.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.plan.AccumulateResponsesRequest;
import io.harness.pms.contracts.plan.AccumulateResponsesResponse;
import io.harness.pms.contracts.plan.AddExecutableResponseRequest;
import io.harness.pms.contracts.plan.AdviserResponseRequest;
import io.harness.pms.contracts.plan.FacilitatorResponseRequest;
import io.harness.pms.contracts.plan.HandleStepResponseRequest;
import io.harness.pms.contracts.plan.NodeExecutionProtoServiceGrpc.NodeExecutionProtoServiceBlockingStub;
import io.harness.pms.contracts.plan.QueueNodeExecutionRequest;
import io.harness.pms.contracts.plan.QueueTaskRequest;
import io.harness.pms.contracts.plan.QueueTaskResponse;
import io.harness.pms.contracts.plan.ResumeNodeExecutionRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.sdk.core.execution.PmsNodeExecutionService;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.io.ResponseDataMapper;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.json.JsonOrchestrationUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class PmsNodeExecutionServiceGrpcImpl implements PmsNodeExecutionService {
  @Inject private NodeExecutionProtoServiceBlockingStub nodeExecutionProtoServiceBlockingStub;
  @Inject private StepRegistry stepRegistry;
  @Inject private ResponseDataMapper responseDataMapper;

  @Override
  public void queueNodeExecution(NodeExecutionProto nodeExecution) {
    nodeExecutionProtoServiceBlockingStub.queueNodeExecution(
        QueueNodeExecutionRequest.newBuilder().setNodeExecution(nodeExecution).build());
  }

  @Override
  public String queueTask(String nodeExecutionId, Map<String, String> setupAbstractions, TaskRequest taskRequest) {
    QueueTaskResponse response = nodeExecutionProtoServiceBlockingStub.queueTask(
        QueueTaskRequest.newBuilder()
            .putAllSetupAbstractions(setupAbstractions == null ? Collections.emptyMap() : setupAbstractions)
            .setTaskRequest(taskRequest)
            .setNodeExecutionId(nodeExecutionId)
            .build());
    return response.getTaskId();
  }

  @Override
  public void addExecutableResponse(
      @NonNull String nodeExecutionId, Status status, ExecutableResponse executableResponse, List<String> callbackIds) {
    AddExecutableResponseRequest.Builder builder = AddExecutableResponseRequest.newBuilder()
                                                       .setNodeExecutionId(nodeExecutionId)
                                                       .setExecutableResponse(executableResponse)
                                                       .addAllCallbackIds(callbackIds);
    if (status != null && status != Status.UNRECOGNIZED) {
      builder.setStatus(status);
    }
    nodeExecutionProtoServiceBlockingStub.addExecutableResponse(builder.build());
  }

  @Override
  public void handleStepResponse(@NonNull String nodeExecutionId, @NonNull StepResponseProto stepResponse) {
    nodeExecutionProtoServiceBlockingStub.handleStepResponse(HandleStepResponseRequest.newBuilder()
                                                                 .setNodeExecutionId(nodeExecutionId)
                                                                 .setStepResponse(stepResponse)
                                                                 .build());
  }

  @Override
  public void resumeNodeExecution(String nodeExecutionId, Map<String, ResponseData> response, boolean asyncError) {
    Map<String, ByteString> responseBytes = responseDataMapper.toResponseDataProto(response);
    nodeExecutionProtoServiceBlockingStub.resumeNodeExecution(ResumeNodeExecutionRequest.newBuilder()
                                                                  .setNodeExecutionId(nodeExecutionId)
                                                                  .putAllResponse(responseBytes)
                                                                  .setAsyncError(asyncError)
                                                                  .build());
  }

  @Override
  public Map<String, ResponseData> accumulateResponses(String planExecutionId, String notifyId) {
    AccumulateResponsesResponse response = nodeExecutionProtoServiceBlockingStub.accumulateResponses(
        AccumulateResponsesRequest.newBuilder().setPlanExecutionId(planExecutionId).setNotifyId(notifyId).build());
    return responseDataMapper.fromResponseDataProto(response.getResponseMap());
  }

  @Override
  public StepParameters extractResolvedStepParameters(NodeExecutionProto nodeExecution) {
    return extractStepParametersInternal(
        nodeExecution.getNode().getStepType(), nodeExecution.getResolvedStepParameters());
  }

  @Override
  public void handleFacilitationResponse(
      @NonNull String nodeExecutionId, @NonNull String notifyId, FacilitatorResponseProto facilitatorResponseProto) {
    nodeExecutionProtoServiceBlockingStub.handleFacilitatorResponse(
        FacilitatorResponseRequest.newBuilder()
            .setFacilitatorResponse(facilitatorResponseProto)
            .setNodeExecutionId(nodeExecutionId)
            .setNotifyId(notifyId)
            .build());
  }

  @Override
  public void handleAdviserResponse(
      @NonNull String nodeExecutionId, @NonNull String notifyId, AdviserResponse adviserResponse) {
    nodeExecutionProtoServiceBlockingStub.handleAdviserResponse(AdviserResponseRequest.newBuilder()
                                                                    .setAdviserResponse(adviserResponse)
                                                                    .setNodeExecutionId(nodeExecutionId)
                                                                    .setNotifyId(notifyId)
                                                                    .build());
  }

  private StepParameters extractStepParametersInternal(StepType stepType, String stepParameters) {
    Step<?> step = stepRegistry.obtain(stepType);
    if (isEmpty(stepParameters)) {
      return null;
    }
    return JsonOrchestrationUtils.asObject(stepParameters, step.getStepParametersClass());
  }
}
