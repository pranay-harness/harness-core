package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.events.AddExecutableResponseRequest;
import io.harness.pms.contracts.execution.events.AdviserResponseRequest;
import io.harness.pms.contracts.execution.events.EventErrorRequest;
import io.harness.pms.contracts.execution.events.FacilitatorResponseRequest;
import io.harness.pms.contracts.execution.events.HandleProgressRequest;
import io.harness.pms.contracts.execution.events.HandleStepResponseRequest;
import io.harness.pms.contracts.execution.events.QueueTaskRequest;
import io.harness.pms.contracts.execution.events.ResumeNodeExecutionRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;
import io.harness.pms.contracts.execution.events.SpawnChildRequest;
import io.harness.pms.contracts.execution.events.SpawnChildrenRequest;
import io.harness.pms.contracts.execution.events.SuspendChainRequest;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.response.publishers.SdkResponseEventPublisher;
import io.harness.pms.sdk.core.steps.io.ResponseDataMapper;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.tasks.ProgressData;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
@Singleton
public class SdkNodeExecutionServiceImpl implements SdkNodeExecutionService {
  @Inject private ResponseDataMapper responseDataMapper;
  @Inject private SdkResponseEventPublisher sdkResponseEventPublisher;

  @Override
  public void suspendChainExecution(Ambiance ambiance, SuspendChainRequest suspendChainRequest) {
    sdkResponseEventPublisher.publishEvent(SdkResponseEventProto.newBuilder()
                                               .setSdkResponseEventType(SdkResponseEventType.SUSPEND_CHAIN)
                                               .setNodeExecutionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance))
                                               .setPlanExecutionId(ambiance.getPlanExecutionId())
                                               .setSuspendChainRequest(suspendChainRequest)
                                               .setAmbiance(ambiance)
                                               .build());
  }

  @Override
  public void addExecutableResponse(Ambiance ambiance, ExecutableResponse executableResponse) {
    AddExecutableResponseRequest executableResponseRequest =
        AddExecutableResponseRequest.newBuilder().setExecutableResponse(executableResponse).build();

    SdkResponseEventProto sdkResponseEvent = SdkResponseEventProto.newBuilder()
                                                 .setSdkResponseEventType(SdkResponseEventType.ADD_EXECUTABLE_RESPONSE)
                                                 .setNodeExecutionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance))
                                                 .setPlanExecutionId(ambiance.getPlanExecutionId())
                                                 .setAddExecutableResponseRequest(executableResponseRequest)
                                                 .setAmbiance(ambiance)
                                                 .build();
    sdkResponseEventPublisher.publishEvent(sdkResponseEvent);
  }

  @Override
  public void handleStepResponse(
      Ambiance ambiance, @NonNull StepResponseProto stepResponse, ExecutableResponse executableResponse) {
    HandleStepResponseRequest.Builder responseRequestBuilder =
        HandleStepResponseRequest.newBuilder().setStepResponse(stepResponse);
    if (executableResponse != null) {
      responseRequestBuilder.setExecutableResponse(executableResponse);
    }
    SdkResponseEventProto sdkResponseEventProto =
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventType(SdkResponseEventType.HANDLE_STEP_RESPONSE)
            .setNodeExecutionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance))
            .setPlanExecutionId(ambiance.getPlanExecutionId())
            .setHandleStepResponseRequest(responseRequestBuilder.build())
            .setAmbiance(ambiance)
            .build();

    sdkResponseEventPublisher.publishEvent(sdkResponseEventProto);
  }

  // This is only for backward comatibility will be removed in next release
  @Override
  public void resumeNodeExecution(Ambiance ambiance, Map<String, ResponseData> response, boolean asyncError) {
    Map<String, ByteString> responseBytes = responseDataMapper.toResponseDataProto(response);
    ResumeNodeExecutionRequest resumeNodeExecutionRequest =
        ResumeNodeExecutionRequest.newBuilder().putAllResponse(responseBytes).setAsyncError(asyncError).build();
    SdkResponseEventProto sdkResponseEvent = SdkResponseEventProto.newBuilder()
                                                 .setSdkResponseEventType(SdkResponseEventType.RESUME_NODE_EXECUTION)
                                                 .setNodeExecutionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance))
                                                 .setPlanExecutionId(ambiance.getPlanExecutionId())
                                                 .setResumeNodeExecutionRequest(resumeNodeExecutionRequest)
                                                 .setAmbiance(ambiance)
                                                 .build();

    sdkResponseEventPublisher.publishEvent(sdkResponseEvent);
  }

  @Override
  public void resumeNodeExecution(
      String planExecutionId, String nodeExecutionId, Map<String, ResponseData> response, boolean asyncError) {
    Map<String, ByteString> responseBytes = responseDataMapper.toResponseDataProto(response);
    ResumeNodeExecutionRequest resumeNodeExecutionRequest =
        ResumeNodeExecutionRequest.newBuilder().putAllResponse(responseBytes).setAsyncError(asyncError).build();
    SdkResponseEventProto sdkResponseEvent = SdkResponseEventProto.newBuilder()
                                                 .setSdkResponseEventType(SdkResponseEventType.RESUME_NODE_EXECUTION)
                                                 .setNodeExecutionId(nodeExecutionId)
                                                 .setPlanExecutionId(planExecutionId)
                                                 .setResumeNodeExecutionRequest(resumeNodeExecutionRequest)
                                                 .build();

    sdkResponseEventPublisher.publishEvent(sdkResponseEvent);
  }

  @Override
  public void handleFacilitationResponse(
      Ambiance ambiance, @NonNull String notifyId, FacilitatorResponseProto facilitatorResponseProto) {
    FacilitatorResponseRequest facilitatorResponseRequest = FacilitatorResponseRequest.newBuilder()
                                                                .setFacilitatorResponse(facilitatorResponseProto)
                                                                .setNotifyId(notifyId)
                                                                .build();

    sdkResponseEventPublisher.publishEvent(SdkResponseEventProto.newBuilder()
                                               .setFacilitatorResponseRequest(facilitatorResponseRequest)
                                               .setSdkResponseEventType(SdkResponseEventType.HANDLE_FACILITATE_RESPONSE)
                                               .setNodeExecutionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance))
                                               .setPlanExecutionId(ambiance.getPlanExecutionId())
                                               .setAmbiance(ambiance)
                                               .build());
  }

  @Override
  public void handleAdviserResponse(Ambiance ambiance, @NonNull String notifyId, AdviserResponse adviserResponse) {
    SdkResponseEventProto handleAdviserResponseRequest =
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventType(SdkResponseEventType.HANDLE_ADVISER_RESPONSE)
            .setNodeExecutionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance))
            .setPlanExecutionId(ambiance.getPlanExecutionId())

            .setAdviserResponseRequest(
                AdviserResponseRequest.newBuilder().setAdviserResponse(adviserResponse).setNotifyId(notifyId).build())
            .setAmbiance(ambiance)
            .build();
    sdkResponseEventPublisher.publishEvent(handleAdviserResponseRequest);
  }

  @Override
  public void handleEventError(NodeExecutionEventType eventType, String eventNotifyId, FailureInfo failureInfo) {
    SdkResponseEventProto handleEventErrorRequest =
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventType(SdkResponseEventType.HANDLE_EVENT_ERROR)

            .setEventErrorRequest(EventErrorRequest.newBuilder()
                                      .setEventNotifyId(eventNotifyId)
                                      .setEventType(eventType)
                                      .setFailureInfo(failureInfo)
                                      .build())

            .build();
    sdkResponseEventPublisher.publishEvent(handleEventErrorRequest);
  }

  @Override
  public void spawnChild(Ambiance ambiance, SpawnChildRequest spawnChildRequest) {
    sdkResponseEventPublisher.publishEvent(SdkResponseEventProto.newBuilder()
                                               .setSdkResponseEventType(SdkResponseEventType.SPAWN_CHILD)
                                               .setNodeExecutionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance))
                                               .setPlanExecutionId(ambiance.getPlanExecutionId())
                                               .setSpawnChildRequest(spawnChildRequest)
                                               .setAmbiance(ambiance)
                                               .build());
  }

  @Override
  public void handleProgressResponse(Ambiance ambiance, ProgressData progressData) {
    String progressJson = RecastOrchestrationUtils.toJson(progressData);
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    sdkResponseEventPublisher.publishEvent(
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventType(SdkResponseEventType.HANDLE_PROGRESS)
            .setNodeExecutionId(nodeExecutionId)
            .setPlanExecutionId(ambiance.getPlanExecutionId())
            .setAmbiance(ambiance)
            .setProgressRequest(HandleProgressRequest.newBuilder().setProgressJson(progressJson).build())

            .build());
  }

  @Override
  public void spawnChildren(Ambiance ambiance, SpawnChildrenRequest spawnChildrenRequest) {
    sdkResponseEventPublisher.publishEvent(SdkResponseEventProto.newBuilder()
                                               .setSdkResponseEventType(SdkResponseEventType.SPAWN_CHILDREN)
                                               .setNodeExecutionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance))
                                               .setPlanExecutionId(ambiance.getPlanExecutionId())
                                               .setSpawnChildrenRequest(spawnChildrenRequest)
                                               .setAmbiance(ambiance)
                                               .build());
  }

  @Override
  public void queueTaskRequest(Ambiance ambiance, QueueTaskRequest queueTaskRequest) {
    sdkResponseEventPublisher.publishEvent(SdkResponseEventProto.newBuilder()
                                               .setSdkResponseEventType(SdkResponseEventType.QUEUE_TASK)
                                               .setNodeExecutionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance))
                                               .setPlanExecutionId(ambiance.getPlanExecutionId())
                                               .setQueueTaskRequest(queueTaskRequest)
                                               .setAmbiance(ambiance)
                                               .build());
  }
}
