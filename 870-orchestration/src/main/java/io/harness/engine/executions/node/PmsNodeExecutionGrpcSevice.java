package io.harness.engine.executions.node;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executables.InvocationHelper;
import io.harness.engine.pms.tasks.TaskExecutor;
import io.harness.engine.progress.EngineProgressCallback;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.plan.AccumulateResponsesRequest;
import io.harness.pms.contracts.plan.AccumulateResponsesResponse;
import io.harness.pms.contracts.plan.AdviserResponseRequest;
import io.harness.pms.contracts.plan.AdviserResponseResponse;
import io.harness.pms.contracts.plan.EventErrorRequest;
import io.harness.pms.contracts.plan.EventErrorResponse;
import io.harness.pms.contracts.plan.FacilitatorResponseRequest;
import io.harness.pms.contracts.plan.FacilitatorResponseResponse;
import io.harness.pms.contracts.plan.NodeExecutionProtoServiceGrpc.NodeExecutionProtoServiceImplBase;
import io.harness.pms.contracts.plan.QueueTaskRequest;
import io.harness.pms.contracts.plan.QueueTaskResponse;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.pms.sdk.core.steps.io.ResponseDataMapper;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.FailureResponseData;
import io.harness.tasks.ResponseData;
import io.harness.waiter.OldNotifyCallback;
import io.harness.waiter.ProgressCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class PmsNodeExecutionGrpcSevice extends NodeExecutionProtoServiceImplBase {
  @Inject private OrchestrationEngine engine;
  @Inject private ResponseDataMapper responseDataMapper;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private Map<TaskCategory, TaskExecutor> taskExecutorMap;
  @Inject private InvocationHelper invocationHelper;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) private String publisherName;

  @Override
  public void queueTask(QueueTaskRequest request, StreamObserver<QueueTaskResponse> responseObserver) {
    try {
      TaskExecutor taskExecutor = taskExecutorMap.get(request.getTaskRequest().getTaskCategory());
      String taskId = Preconditions.checkNotNull(
          taskExecutor.queueTask(request.getSetupAbstractionsMap(), request.getTaskRequest(), Duration.ofSeconds(0)));
      OldNotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(request.getNodeExecutionId()).build();
      ProgressCallback progressCallback =
          EngineProgressCallback.builder().nodeExecutionId(request.getNodeExecutionId()).build();
      waitNotifyEngine.waitForAllOn(publisherName, callback, progressCallback, taskId);
      responseObserver.onNext(QueueTaskResponse.newBuilder().setTaskId(taskId).build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException ex) {
      log.error("Error while queuing delegate task", ex);
      responseObserver.onError(ex);
    } catch (Exception ex) {
      log.error("Error while queuing delegate task", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void accumulateResponses(
      AccumulateResponsesRequest request, StreamObserver<AccumulateResponsesResponse> responseObserver) {
    Map<String, ResponseData> responseDataMap =
        invocationHelper.accumulateResponses(request.getPlanExecutionId(), request.getNotifyId());
    Map<String, ByteString> response = responseDataMapper.toResponseDataProto(responseDataMap);
    responseObserver.onNext(AccumulateResponsesResponse.newBuilder().putAllResponse(response).build());
    responseObserver.onCompleted();
  }

  @Override
  public void handleFacilitatorResponse(
      FacilitatorResponseRequest request, StreamObserver<FacilitatorResponseResponse> responseObserver) {
    waitNotifyEngine.doneWith(request.getNotifyId(),
        BinaryResponseData.builder().data(request.getFacilitatorResponse().toByteArray()).build());

    responseObserver.onNext(FacilitatorResponseResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void handleAdviserResponse(
      AdviserResponseRequest request, StreamObserver<AdviserResponseResponse> responseObserver) {
    waitNotifyEngine.doneWith(
        request.getNotifyId(), BinaryResponseData.builder().data(request.getAdviserResponse().toByteArray()).build());
    responseObserver.onNext(AdviserResponseResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void handleEventError(EventErrorRequest request, StreamObserver<EventErrorResponse> responseObserver) {
    waitNotifyEngine.doneWith(request.getEventNotifyId(),
        FailureResponseData.builder()
            .errorMessage(request.getFailureInfo().getErrorMessage())
            .failureTypes(
                EngineExceptionUtils.transformToWingsFailureTypes(request.getFailureInfo().getFailureTypesList()))
            .build());

    responseObserver.onNext(EventErrorResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
