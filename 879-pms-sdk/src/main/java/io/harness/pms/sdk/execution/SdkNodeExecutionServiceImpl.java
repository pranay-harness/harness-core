package io.harness.pms.sdk.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;
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
import io.harness.pms.contracts.execution.events.QueueNodeExecutionRequest;
import io.harness.pms.contracts.execution.events.QueueTaskRequest;
import io.harness.pms.contracts.execution.events.QueueTaskRequestAndExecutableResponseRequest;
import io.harness.pms.contracts.execution.events.ResumeNodeExecutionRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.plan.AccumulateResponsesRequest;
import io.harness.pms.contracts.plan.AccumulateResponsesResponse;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.contracts.plan.NodeExecutionProtoServiceGrpc.NodeExecutionProtoServiceBlockingStub;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.execution.SdkResponseEvent;
import io.harness.pms.execution.SdkResponseEventInternal;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.io.ResponseDataMapper;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.response.events.SdkResponseEventPublisher;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.tasks.ResponseData;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class SdkNodeExecutionServiceImpl implements SdkNodeExecutionService {
  @Inject private NodeExecutionProtoServiceBlockingStub nodeExecutionProtoServiceBlockingStub;
  @Inject private StepRegistry stepRegistry;
  @Inject private ResponseDataMapper responseDataMapper;
  @Inject private SdkResponseEventPublisher sdkResponseEventPublisher;

  @Override
  public void queueNodeExecution(NodeExecutionProto nodeExecution) {
    SdkResponseEventInternal sdkResponseEventInternal =
        SdkResponseEventInternal.builder()
            .sdkResponseEventType(SdkResponseEventType.QUEUE_NODE)
            .sdkResponseEventRequest(
                SdkResponseEventRequest.newBuilder()
                    .setNodeExecutionId(nodeExecution.getUuid())
                    .setQueueNodeExecutionRequest(
                        QueueNodeExecutionRequest.newBuilder().setNodeExecution(nodeExecution).build())
                    .build())
            .build();
    sdkResponseEventPublisher.send(SdkResponseEvent.builder()
                                       .sdkResponseEventInternals(Collections.singletonList(sdkResponseEventInternal))
                                       .build());
  }

  @Override
  public void queueNodeExecutionAndAddExecutableResponse(String currentNodeExecutionId,
      QueueNodeExecutionRequest queueNodeExecutionRequest, AddExecutableResponseRequest addExecutableResponseRequest) {
    SdkResponseEventInternal queueNodeExecution =
        SdkResponseEventInternal.builder()
            .sdkResponseEventType(SdkResponseEventType.QUEUE_NODE)
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                         .setNodeExecutionId(currentNodeExecutionId)
                                         .setQueueNodeExecutionRequest(queueNodeExecutionRequest)
                                         .build())
            .build();

    SdkResponseEventInternal addExecutionRequestInternal =
        SdkResponseEventInternal.builder()
            .sdkResponseEventType(SdkResponseEventType.ADD_EXECUTABLE_RESPONSE)
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                         .setNodeExecutionId(currentNodeExecutionId)
                                         .setAddExecutableResponseRequest(addExecutableResponseRequest)
                                         .build())
            .build();
    sdkResponseEventPublisher.send(
        SdkResponseEvent.builder()
            .sdkResponseEventInternals(Lists.newArrayList(queueNodeExecution, addExecutionRequestInternal))
            .build());
  }

  @Override
  public void addExecutableResponseAndResumeNode(String currentNodeExecutionId,
      AddExecutableResponseRequest addExecutableResponseRequest,
      ResumeNodeExecutionRequest resumeNodeExecutionRequest) {
    SdkResponseEventInternal queueNodeExecution =
        SdkResponseEventInternal.builder()
            .sdkResponseEventType(SdkResponseEventType.ADD_EXECUTABLE_RESPONSE)
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                         .setNodeExecutionId(currentNodeExecutionId)
                                         .setAddExecutableResponseRequest(addExecutableResponseRequest)
                                         .build())
            .build();

    SdkResponseEventInternal addExecutionRequestInternal =
        SdkResponseEventInternal.builder()
            .sdkResponseEventType(SdkResponseEventType.RESUME_NODE_EXECUTION)
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                         .setNodeExecutionId(currentNodeExecutionId)
                                         .setResumeNodeExecutionRequest(resumeNodeExecutionRequest)
                                         .build())
            .build();
    sdkResponseEventPublisher.send(
        SdkResponseEvent.builder()
            .sdkResponseEventInternals(Lists.newArrayList(queueNodeExecution, addExecutionRequestInternal))
            .build());
  }

  @Override
  public void queueTaskAndAddExecutableResponse(
      QueueTaskRequest queueTaskRequest, AddExecutableResponseRequest addExecutableResponseRequest) {
    SdkResponseEventInternal sdkResponseEventInternal =
        SdkResponseEventInternal.builder()
            .sdkResponseEventType(SdkResponseEventType.QUEUE_TASK_AND_ADD_EXECUTABLE_RESPONSE)
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                         .setQueueTaskRequestAndExecutableResponseRequest(
                                             QueueTaskRequestAndExecutableResponseRequest.newBuilder()
                                                 .setAddExecutableResponseRequest(addExecutableResponseRequest)
                                                 .setQueueTaskRequest(queueTaskRequest)
                                                 .build())
                                         .build())
            .build();

    sdkResponseEventPublisher.send(SdkResponseEvent.builder()
                                       .sdkResponseEventInternals(Collections.singletonList(sdkResponseEventInternal))
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
    SdkResponseEventInternal sdkResponseEventInternal =
        SdkResponseEventInternal.builder()
            .sdkResponseEventType(SdkResponseEventType.ADD_EXECUTABLE_RESPONSE)
            .sdkResponseEventRequest(
                SdkResponseEventRequest.newBuilder().setAddExecutableResponseRequest(builder.build()).build())
            .build();
    sdkResponseEventPublisher.send(SdkResponseEvent.builder()
                                       .sdkResponseEventInternals(Collections.singletonList(sdkResponseEventInternal))
                                       .build());
  }

  @Override
  public void handleStepResponse(@NonNull String nodeExecutionId, @NonNull StepResponseProto stepResponse) {
    HandleStepResponseRequest responseRequest = HandleStepResponseRequest.newBuilder()
                                                    .setNodeExecutionId(nodeExecutionId)
                                                    .setStepResponse(stepResponse)
                                                    .build();
    SdkResponseEventInternal sdkResponseEventInternal =
        SdkResponseEventInternal.builder()
            .sdkResponseEventType(SdkResponseEventType.HANDLE_STEP_RESPONSE)
            .sdkResponseEventRequest(
                SdkResponseEventRequest.newBuilder().setHandleStepResponseRequest(responseRequest).build())
            .build();

    sdkResponseEventPublisher.send(SdkResponseEvent.builder()
                                       .sdkResponseEventInternals(Collections.singletonList(sdkResponseEventInternal))
                                       .build());
  }

  @Override
  public void resumeNodeExecution(String nodeExecutionId, Map<String, ResponseData> response, boolean asyncError) {
    Map<String, ByteString> responseBytes = responseDataMapper.toResponseDataProto(response);
    ResumeNodeExecutionRequest resumeNodeExecutionRequest = ResumeNodeExecutionRequest.newBuilder()
                                                                .setNodeExecutionId(nodeExecutionId)
                                                                .putAllResponse(responseBytes)
                                                                .setAsyncError(asyncError)
                                                                .build();
    SdkResponseEventInternal sdkResponseEventInternal =
        SdkResponseEventInternal.builder()
            .sdkResponseEventType(SdkResponseEventType.RESUME_NODE_EXECUTION)
            .sdkResponseEventRequest(
                SdkResponseEventRequest.newBuilder().setResumeNodeExecutionRequest(resumeNodeExecutionRequest).build())
            .build();

    sdkResponseEventPublisher.send(SdkResponseEvent.builder()
                                       .sdkResponseEventInternals(Collections.singletonList(sdkResponseEventInternal))
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
    FacilitatorResponseRequest facilitatorResponseRequest = FacilitatorResponseRequest.newBuilder()
                                                                .setFacilitatorResponse(facilitatorResponseProto)
                                                                .setNodeExecutionId(nodeExecutionId)
                                                                .setNotifyId(notifyId)
                                                                .build();

    sdkResponseEventPublisher.send(
        SdkResponseEvent.builder()
            .sdkResponseEventInternals(Collections.singletonList(
                SdkResponseEventInternal.builder()
                    .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                                 .setFacilitatorResponseRequest(facilitatorResponseRequest)
                                                 .build())
                    .sdkResponseEventType(SdkResponseEventType.HANDLE_FACILITATE_RESPONSE)
                    .build()))
            .build());
  }

  @Override
  public void handleAdviserResponse(
      @NonNull String nodeExecutionId, @NonNull String notifyId, AdviserResponse adviserResponse) {
    SdkResponseEventInternal handleAdviserResponseRequest =
        SdkResponseEventInternal.builder()
            .sdkResponseEventType(SdkResponseEventType.HANDLE_ADVISER_RESPONSE)
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                         .setAdviserResponseRequest(AdviserResponseRequest.newBuilder()
                                                                        .setAdviserResponse(adviserResponse)
                                                                        .setNodeExecutionId(nodeExecutionId)
                                                                        .setNotifyId(notifyId)
                                                                        .build())
                                         .build())
            .build();
    sdkResponseEventPublisher.send(
        SdkResponseEvent.builder().sdkResponseEventInternals(Lists.newArrayList(handleAdviserResponseRequest)).build());
  }

  @Override
  public void handleEventError(NodeExecutionEventType eventType, String eventNotifyId, FailureInfo failureInfo) {
    SdkResponseEventInternal handleEventErrorRequest =
        SdkResponseEventInternal.builder()
            .sdkResponseEventType(SdkResponseEventType.HANDLE_EVENT_ERROR)
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                         .setEventErrorRequest(EventErrorRequest.newBuilder()
                                                                   .setEventNotifyId(eventNotifyId)
                                                                   .setEventType(eventType)
                                                                   .setFailureInfo(failureInfo)
                                                                   .build())
                                         .build())
            .build();
    sdkResponseEventPublisher.send(
        SdkResponseEvent.builder().sdkResponseEventInternals(Lists.newArrayList(handleEventErrorRequest)).build());
  }

  private StepParameters extractStepParametersInternal(StepType stepType, String stepParameters) {
    Step<?> step = stepRegistry.obtain(stepType);
    if (isEmpty(stepParameters)) {
      return null;
    }
    return RecastOrchestrationUtils.fromDocumentJson(stepParameters, step.getStepParametersClass());
  }
}
