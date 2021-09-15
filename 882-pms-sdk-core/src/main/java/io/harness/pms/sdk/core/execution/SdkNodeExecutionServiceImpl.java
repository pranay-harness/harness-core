/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

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
  public void suspendChainExecution(
      String planExecutionId, String currentNodeExecutionId, SuspendChainRequest suspendChainRequest) {
    sdkResponseEventPublisher.publishEvent(SdkResponseEventProto.newBuilder()
                                               .setSdkResponseEventType(SdkResponseEventType.SUSPEND_CHAIN)
                                               .setNodeExecutionId(currentNodeExecutionId)
                                               .setPlanExecutionId(planExecutionId)
                                               .setSuspendChainRequest(suspendChainRequest)
                                               .build());
  }

  @Override
  public void addExecutableResponse(
      @NonNull String planExecutionId, @NonNull String nodeExecutionId, ExecutableResponse executableResponse) {
    AddExecutableResponseRequest executableResponseRequest =
        AddExecutableResponseRequest.newBuilder().setExecutableResponse(executableResponse).build();

    SdkResponseEventProto sdkResponseEvent = SdkResponseEventProto.newBuilder()
                                                 .setSdkResponseEventType(SdkResponseEventType.ADD_EXECUTABLE_RESPONSE)
                                                 .setNodeExecutionId(nodeExecutionId)
                                                 .setPlanExecutionId(planExecutionId)
                                                 .setAddExecutableResponseRequest(executableResponseRequest)
                                                 .build();
    sdkResponseEventPublisher.publishEvent(sdkResponseEvent);
  }

  @Override
  public void handleStepResponse(String planExecutionId, @NonNull String nodeExecutionId,
      @NonNull StepResponseProto stepResponse, ExecutableResponse executableResponse) {
    HandleStepResponseRequest.Builder responseRequestBuilder =
        HandleStepResponseRequest.newBuilder().setStepResponse(stepResponse);
    if (executableResponse != null) {
      responseRequestBuilder.setExecutableResponse(executableResponse);
    }
    SdkResponseEventProto sdkResponseEventProto =
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventType(SdkResponseEventType.HANDLE_STEP_RESPONSE)
            .setNodeExecutionId(nodeExecutionId)
            .setPlanExecutionId(planExecutionId)
            .setHandleStepResponseRequest(responseRequestBuilder.build())
            .build();

    sdkResponseEventPublisher.publishEvent(sdkResponseEventProto);
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
  public void handleFacilitationResponse(String planExecutionId, @NonNull String nodeExecutionId,
      @NonNull String notifyId, FacilitatorResponseProto facilitatorResponseProto) {
    FacilitatorResponseRequest facilitatorResponseRequest = FacilitatorResponseRequest.newBuilder()
                                                                .setFacilitatorResponse(facilitatorResponseProto)
                                                                .setNotifyId(notifyId)
                                                                .build();

    sdkResponseEventPublisher.publishEvent(SdkResponseEventProto.newBuilder()
                                               .setFacilitatorResponseRequest(facilitatorResponseRequest)
                                               .setSdkResponseEventType(SdkResponseEventType.HANDLE_FACILITATE_RESPONSE)
                                               .setPlanExecutionId(planExecutionId)
                                               .setNodeExecutionId(nodeExecutionId)
                                               .build());
  }

  @Override
  public void handleAdviserResponse(String planExecutionId, @NonNull String nodeExecutionId, @NonNull String notifyId,
      AdviserResponse adviserResponse) {
    SdkResponseEventProto handleAdviserResponseRequest =
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventType(SdkResponseEventType.HANDLE_ADVISER_RESPONSE)
            .setNodeExecutionId(nodeExecutionId)
            .setPlanExecutionId(planExecutionId)

            .setAdviserResponseRequest(
                AdviserResponseRequest.newBuilder().setAdviserResponse(adviserResponse).setNotifyId(notifyId).build())

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
  public void spawnChild(String planExecutionId, String nodeExecutionId, SpawnChildRequest spawnChildRequest) {
    sdkResponseEventPublisher.publishEvent(SdkResponseEventProto.newBuilder()
                                               .setSdkResponseEventType(SdkResponseEventType.SPAWN_CHILD)
                                               .setNodeExecutionId(nodeExecutionId)
                                               .setPlanExecutionId(planExecutionId)
                                               .setSpawnChildRequest(spawnChildRequest)
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

            .setProgressRequest(HandleProgressRequest.newBuilder().setProgressJson(progressJson).build())

            .build());
  }

  @Override
  public void spawnChildren(String planExecutionId, String nodeExecutionId, SpawnChildrenRequest spawnChildrenRequest) {
    sdkResponseEventPublisher.publishEvent(SdkResponseEventProto.newBuilder()
                                               .setSdkResponseEventType(SdkResponseEventType.SPAWN_CHILDREN)
                                               .setNodeExecutionId(nodeExecutionId)
                                               .setPlanExecutionId(planExecutionId)
                                               .setSpawnChildrenRequest(spawnChildrenRequest)
                                               .build());
  }

  @Override
  public void queueTaskRequest(String planExecutionId, String nodeExecutionId, QueueTaskRequest queueTaskRequest) {
    sdkResponseEventPublisher.publishEvent(SdkResponseEventProto.newBuilder()
                                               .setSdkResponseEventType(SdkResponseEventType.QUEUE_TASK)
                                               .setNodeExecutionId(nodeExecutionId)
                                               .setPlanExecutionId(planExecutionId)
                                               .setQueueTaskRequest(queueTaskRequest)
                                               .build());
  }
}
