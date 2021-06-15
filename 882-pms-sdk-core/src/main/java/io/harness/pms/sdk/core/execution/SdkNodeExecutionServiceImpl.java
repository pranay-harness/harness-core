package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.AddExecutableResponseRequest;
import io.harness.pms.contracts.execution.events.AdviserResponseRequest;
import io.harness.pms.contracts.execution.events.EventErrorRequest;
import io.harness.pms.contracts.execution.events.FacilitatorResponseRequest;
import io.harness.pms.contracts.execution.events.HandleProgressRequest;
import io.harness.pms.contracts.execution.events.HandleStepResponseRequest;
import io.harness.pms.contracts.execution.events.QueueTaskRequest;
import io.harness.pms.contracts.execution.events.ResumeNodeExecutionRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
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
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.response.publishers.SdkResponseEventPublisher;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.io.ResponseDataMapper;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.tasks.ProgressData;
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
    sdkResponseEventPublisher.publishEvent(
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventType(SdkResponseEventType.SUSPEND_CHAIN)
            .setSdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
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
    SdkResponseEventProto sdkResponseEvent =
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventType(SdkResponseEventType.ADD_EXECUTABLE_RESPONSE)
            .setSdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                            .setNodeExecutionId(nodeExecutionId)
                                            .setAddExecutableResponseRequest(builder.build())
                                            .build())
            .build();
    sdkResponseEventPublisher.publishEvent(sdkResponseEvent);
  }

  @Override
  public void handleStepResponse(@NonNull String nodeExecutionId, @NonNull StepResponseProto stepResponse) {
    HandleStepResponseRequest responseRequest = HandleStepResponseRequest.newBuilder()
                                                    .setNodeExecutionId(nodeExecutionId)
                                                    .setStepResponse(stepResponse)
                                                    .build();
    SdkResponseEventProto sdkResponseEventProto =
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventType(SdkResponseEventType.HANDLE_STEP_RESPONSE)
            .setSdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                            .setNodeExecutionId(nodeExecutionId)
                                            .setHandleStepResponseRequest(responseRequest)
                                            .build())
            .build();

    sdkResponseEventPublisher.publishEvent(sdkResponseEventProto);
  }

  @Override
  public void resumeNodeExecution(String nodeExecutionId, Map<String, ResponseData> response, boolean asyncError) {
    Map<String, ByteString> responseBytes = responseDataMapper.toResponseDataProto(response);
    ResumeNodeExecutionRequest resumeNodeExecutionRequest = ResumeNodeExecutionRequest.newBuilder()
                                                                .setNodeExecutionId(nodeExecutionId)
                                                                .putAllResponse(responseBytes)
                                                                .setAsyncError(asyncError)
                                                                .build();
    SdkResponseEventProto sdkResponseEvent =
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventType(SdkResponseEventType.RESUME_NODE_EXECUTION)
            .setSdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                            .setNodeExecutionId(nodeExecutionId)
                                            .setResumeNodeExecutionRequest(resumeNodeExecutionRequest)
                                            .build())
            .build();

    sdkResponseEventPublisher.publishEvent(sdkResponseEvent);
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

    sdkResponseEventPublisher.publishEvent(
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                            .setNodeExecutionId(nodeExecutionId)
                                            .setFacilitatorResponseRequest(facilitatorResponseRequest)
                                            .build())
            .setSdkResponseEventType(SdkResponseEventType.HANDLE_FACILITATE_RESPONSE)
            .build());
  }

  @Override
  public void handleAdviserResponse(
      @NonNull String nodeExecutionId, @NonNull String notifyId, AdviserResponse adviserResponse) {
    SdkResponseEventProto handleAdviserResponseRequest =
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventType(SdkResponseEventType.HANDLE_ADVISER_RESPONSE)
            .setSdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                            .setNodeExecutionId(nodeExecutionId)
                                            .setAdviserResponseRequest(AdviserResponseRequest.newBuilder()
                                                                           .setAdviserResponse(adviserResponse)
                                                                           .setNodeExecutionId(nodeExecutionId)
                                                                           .setNotifyId(notifyId)
                                                                           .build())
                                            .build())
            .build();
    sdkResponseEventPublisher.publishEvent(handleAdviserResponseRequest);
  }

  @Override
  public void handleEventError(NodeExecutionEventType eventType, String eventNotifyId, FailureInfo failureInfo) {
    SdkResponseEventProto handleEventErrorRequest =
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventType(SdkResponseEventType.HANDLE_EVENT_ERROR)
            .setSdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                            .setEventErrorRequest(EventErrorRequest.newBuilder()
                                                                      .setEventNotifyId(eventNotifyId)
                                                                      .setEventType(eventType)
                                                                      .setFailureInfo(failureInfo)
                                                                      .build())
                                            .build())
            .build();
    sdkResponseEventPublisher.publishEvent(handleEventErrorRequest);
  }

  @Override
  public void spawnChild(SpawnChildRequest spawnChildRequest) {
    sdkResponseEventPublisher.publishEvent(
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventType(SdkResponseEventType.SPAWN_CHILD)
            .setSdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                            .setSpawnChildRequest(spawnChildRequest)
                                            .setNodeExecutionId(spawnChildRequest.getNodeExecutionId())
                                            .build())
            .build());
  }

  @Override
  public void handleProgressResponse(Ambiance ambiance, ProgressData progressData) {
    String progressJson = RecastOrchestrationUtils.toDocumentJson(progressData);
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    sdkResponseEventPublisher.publishEvent(
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventType(SdkResponseEventType.HANDLE_PROGRESS)
            .setSdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                            .setProgressRequest(HandleProgressRequest.newBuilder()
                                                                    .setNodeExecutionId(nodeExecutionId)
                                                                    .setPlanExecutionId(ambiance.getPlanExecutionId())
                                                                    .setProgressJson(progressJson)
                                                                    .build())
                                            .setNodeExecutionId(nodeExecutionId)
                                            .build())
            .build());
  }

  @Override
  public void spawnChildren(SpawnChildrenRequest spawnChildrenRequest) {
    sdkResponseEventPublisher.publishEvent(
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventType(SdkResponseEventType.SPAWN_CHILDREN)
            .setSdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                            .setSpawnChildrenRequest(spawnChildrenRequest)
                                            .setNodeExecutionId(spawnChildrenRequest.getNodeExecutionId())
                                            .build())
            .build());
  }

  @Override
  public void queueTaskRequest(QueueTaskRequest queueTaskRequest) {
    sdkResponseEventPublisher.publishEvent(
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventType(SdkResponseEventType.QUEUE_TASK)
            .setSdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
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
