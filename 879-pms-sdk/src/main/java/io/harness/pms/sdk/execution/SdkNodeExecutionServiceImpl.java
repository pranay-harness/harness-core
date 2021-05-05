package io.harness.pms.sdk.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.AddExecutableResponseRequest;
import io.harness.pms.contracts.execution.events.AdviserResponseRequest;
import io.harness.pms.contracts.execution.events.EventErrorRequest;
import io.harness.pms.contracts.execution.events.FacilitatorResponseRequest;
import io.harness.pms.contracts.execution.events.HandleStepResponseRequest;
import io.harness.pms.contracts.execution.events.QueueTaskRequest;
import io.harness.pms.contracts.execution.events.ResumeNodeExecutionRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;
import io.harness.pms.contracts.execution.events.SpawnChildRequest;
import io.harness.pms.contracts.execution.events.SpawnChildrenRequest;
import io.harness.pms.contracts.execution.events.SuspendChainRequest;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.execution.SdkResponseEvent;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.io.ResponseDataMapper;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.response.events.SdkResponseEventPublisher;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
@Singleton
public class SdkNodeExecutionServiceImpl implements SdkNodeExecutionService {
  @Inject private StepRegistry stepRegistry;
  @Inject private ResponseDataMapper responseDataMapper;
  @Inject private SdkResponseEventPublisher sdkResponseEventPublisher;

  @Override
  public void suspendChainExecution(String currentNodeExecutionId, SuspendChainRequest suspendChainRequest) {
    sdkResponseEventPublisher.send(SdkResponseEvent.builder()
                                       .sdkResponseEventType(SdkResponseEventType.SUSPEND_CHAIN)
                                       .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                                                    .setNodeExecutionId(currentNodeExecutionId)
                                                                    .setSuspendChainRequest(suspendChainRequest)
                                                                    .build())
                                       .build());
  }

  @Override
  public void addExecutableResponse(
      @NonNull String nodeExecutionId, Status status, ExecutableResponse executableResponse, List<String> callbackIds) {
    AddExecutableResponseRequest.Builder builder = AddExecutableResponseRequest.newBuilder()
                                                       .setNodeExecutionId(nodeExecutionId)
                                                       .setExecutableResponse(executableResponse)
                                                       .addAllCallbackIds(callbackIds);
    if (status != null && status != Status.NO_OP) {
      builder.setStatus(status);
    }
    SdkResponseEvent sdkResponseEvent =
        SdkResponseEvent.builder()
            .sdkResponseEventType(SdkResponseEventType.ADD_EXECUTABLE_RESPONSE)
            .sdkResponseEventRequest(
                SdkResponseEventRequest.newBuilder().setAddExecutableResponseRequest(builder.build()).build())
            .build();
    sdkResponseEventPublisher.send(sdkResponseEvent);
  }

  @Override
  public void handleStepResponse(@NonNull String nodeExecutionId, @NonNull StepResponseProto stepResponse) {
    HandleStepResponseRequest responseRequest = HandleStepResponseRequest.newBuilder()
                                                    .setNodeExecutionId(nodeExecutionId)
                                                    .setStepResponse(stepResponse)
                                                    .build();
    SdkResponseEvent sdkResponseEvent =
        SdkResponseEvent.builder()
            .sdkResponseEventType(SdkResponseEventType.HANDLE_STEP_RESPONSE)
            .sdkResponseEventRequest(
                SdkResponseEventRequest.newBuilder().setHandleStepResponseRequest(responseRequest).build())
            .build();

    sdkResponseEventPublisher.send(sdkResponseEvent);
  }

  @Override
  public void resumeNodeExecution(String nodeExecutionId, Map<String, ResponseData> response, boolean asyncError) {
    Map<String, ByteString> responseBytes = responseDataMapper.toResponseDataProto(response);
    ResumeNodeExecutionRequest resumeNodeExecutionRequest = ResumeNodeExecutionRequest.newBuilder()
                                                                .setNodeExecutionId(nodeExecutionId)
                                                                .putAllResponse(responseBytes)
                                                                .setAsyncError(asyncError)
                                                                .build();
    SdkResponseEvent sdkResponseEvent =
        SdkResponseEvent.builder()
            .sdkResponseEventType(SdkResponseEventType.RESUME_NODE_EXECUTION)
            .sdkResponseEventRequest(
                SdkResponseEventRequest.newBuilder().setResumeNodeExecutionRequest(resumeNodeExecutionRequest).build())
            .build();

    sdkResponseEventPublisher.send(sdkResponseEvent);
  }

  @Override
  public StepParameters extractResolvedStepParameters(NodeExecutionProto nodeExecution) {
    return extractStepParametersInternal(
        nodeExecution.getNode().getStepType(), nodeExecution.getResolvedStepParameters());
  }

  @Override
  public void handleFacilitationResponse(
      @NonNull String nodeExecutionId, @NonNull String notifyId, FacilitatorResponseProto facilitatorResponseProto) {
    FacilitatorResponseRequest facilitatorResponseRequest = FacilitatorResponseRequest.newBuilder()
                                                                .setFacilitatorResponse(facilitatorResponseProto)
                                                                .setNodeExecutionId(nodeExecutionId)
                                                                .setNotifyId(notifyId)
                                                                .build();

    sdkResponseEventPublisher.send(
        SdkResponseEvent.builder()
            .sdkResponseEventRequest(
                SdkResponseEventRequest.newBuilder().setFacilitatorResponseRequest(facilitatorResponseRequest).build())
            .sdkResponseEventType(SdkResponseEventType.HANDLE_FACILITATE_RESPONSE)
            .build());
  }

  @Override
  public void handleAdviserResponse(
      @NonNull String nodeExecutionId, @NonNull String notifyId, AdviserResponse adviserResponse) {
    SdkResponseEvent handleAdviserResponseRequest =
        SdkResponseEvent.builder()
            .sdkResponseEventType(SdkResponseEventType.HANDLE_ADVISER_RESPONSE)
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                         .setAdviserResponseRequest(AdviserResponseRequest.newBuilder()
                                                                        .setAdviserResponse(adviserResponse)
                                                                        .setNodeExecutionId(nodeExecutionId)
                                                                        .setNotifyId(notifyId)
                                                                        .build())
                                         .build())
            .build();
    sdkResponseEventPublisher.send(handleAdviserResponseRequest);
  }

  @Override
  public void handleEventError(NodeExecutionEventType eventType, String eventNotifyId, FailureInfo failureInfo) {
    SdkResponseEvent handleEventErrorRequest =
        SdkResponseEvent.builder()
            .sdkResponseEventType(SdkResponseEventType.HANDLE_EVENT_ERROR)
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                         .setEventErrorRequest(EventErrorRequest.newBuilder()
                                                                   .setEventNotifyId(eventNotifyId)
                                                                   .setEventType(eventType)
                                                                   .setFailureInfo(failureInfo)
                                                                   .build())
                                         .build())
            .build();
    sdkResponseEventPublisher.send(handleEventErrorRequest);
  }

  @Override
  public void spawnChild(SpawnChildRequest spawnChildRequest) {
    sdkResponseEventPublisher.send(
        SdkResponseEvent.builder()
            .sdkResponseEventType(SdkResponseEventType.SPAWN_CHILD)
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                         .setSpawnChildRequest(spawnChildRequest)
                                         .setNodeExecutionId(spawnChildRequest.getNodeExecutionId())
                                         .build())
            .build());
  }

  @Override
  public void spawnChildren(SpawnChildrenRequest spawnChildrenRequest) {
    sdkResponseEventPublisher.send(
        SdkResponseEvent.builder()
            .sdkResponseEventType(SdkResponseEventType.SPAWN_CHILDREN)
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                         .setSpawnChildrenRequest(spawnChildrenRequest)
                                         .setNodeExecutionId(spawnChildrenRequest.getNodeExecutionId())
                                         .build())
            .build());
  }

  @Override
  public void queueTaskRequest(QueueTaskRequest queueTaskRequest) {
    sdkResponseEventPublisher.send(
        SdkResponseEvent.builder()
            .sdkResponseEventType(SdkResponseEventType.QUEUE_TASK)
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                         .setQueueTaskRequest(queueTaskRequest)
                                         .setNodeExecutionId(queueTaskRequest.getNodeExecutionId())
                                         .build())
            .build());
  }

  private StepParameters extractStepParametersInternal(StepType stepType, String stepParameters) {
    Step<?> step = stepRegistry.obtain(stepType);
    if (isEmpty(stepParameters)) {
      return null;
    }
    return RecastOrchestrationUtils.fromDocumentJson(stepParameters, step.getStepParametersClass());
  }
}
