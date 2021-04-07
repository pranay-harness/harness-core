package io.harness.engine.executions.node;

import io.harness.engine.OrchestrationEngine;
import io.harness.pms.contracts.plan.AccumulateResponsesRequest;
import io.harness.pms.contracts.plan.AccumulateResponsesResponse;
import io.harness.pms.contracts.plan.AddExecutableResponseRequest;
import io.harness.pms.contracts.plan.AddExecutableResponseResponse;
import io.harness.pms.contracts.plan.AdviserResponseRequest;
import io.harness.pms.contracts.plan.AdviserResponseResponse;
import io.harness.pms.contracts.plan.EventErrorRequest;
import io.harness.pms.contracts.plan.EventErrorResponse;
import io.harness.pms.contracts.plan.FacilitatorResponseRequest;
import io.harness.pms.contracts.plan.FacilitatorResponseResponse;
import io.harness.pms.contracts.plan.HandleStepResponseRequest;
import io.harness.pms.contracts.plan.HandleStepResponseResponse;
import io.harness.pms.contracts.plan.NodeExecutionProtoServiceGrpc.NodeExecutionProtoServiceImplBase;
import io.harness.pms.contracts.plan.QueueTaskRequest;
import io.harness.pms.contracts.plan.QueueTaskResponse;
import io.harness.pms.contracts.plan.ResumeNodeExecutionRequest;
import io.harness.pms.contracts.plan.ResumeNodeExecutionResponse;
import io.harness.pms.sdk.core.steps.io.ResponseDataMapper;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class PmsNodeExecutionGrpcSevice extends NodeExecutionProtoServiceImplBase {
  @Inject private OrchestrationEngine engine;
  @Inject private PmsNodeExecutionServiceImpl pmsNodeExecutionService;
  @Inject private ResponseDataMapper responseDataMapper;

  @Override
  public void queueTask(QueueTaskRequest request, StreamObserver<QueueTaskResponse> responseObserver) {
    try {
      String taskId = pmsNodeExecutionService.queueTask(
          request.getNodeExecutionId(), request.getSetupAbstractionsMap(), request.getTaskRequest());
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
  public void addExecutableResponse(
      AddExecutableResponseRequest request, StreamObserver<AddExecutableResponseResponse> responseObserver) {
    pmsNodeExecutionService.addExecutableResponse(request.getNodeExecutionId(), request.getStatus(),
        request.getExecutableResponse(), request.getCallbackIdsList());
    responseObserver.onNext(AddExecutableResponseResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void handleStepResponse(
      HandleStepResponseRequest request, StreamObserver<HandleStepResponseResponse> responseObserver) {
    pmsNodeExecutionService.handleStepResponse(request.getNodeExecutionId(), request.getStepResponse());
    responseObserver.onNext(HandleStepResponseResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void resumeNodeExecution(
      ResumeNodeExecutionRequest request, StreamObserver<ResumeNodeExecutionResponse> responseObserver) {
    engine.resume(request.getNodeExecutionId(), request.getResponseMap(), request.getAsyncError());
    responseObserver.onNext(ResumeNodeExecutionResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void accumulateResponses(
      AccumulateResponsesRequest request, StreamObserver<AccumulateResponsesResponse> responseObserver) {
    Map<String, ResponseData> responseDataMap =
        pmsNodeExecutionService.accumulateResponses(request.getPlanExecutionId(), request.getNotifyId());
    Map<String, ByteString> response = responseDataMapper.toResponseDataProto(responseDataMap);
    responseObserver.onNext(AccumulateResponsesResponse.newBuilder().putAllResponse(response).build());
    responseObserver.onCompleted();
  }

  @Override
  public void handleFacilitatorResponse(
      FacilitatorResponseRequest request, StreamObserver<FacilitatorResponseResponse> responseObserver) {
    pmsNodeExecutionService.handleFacilitationResponse(
        request.getNodeExecutionId(), request.getNotifyId(), request.getFacilitatorResponse());

    responseObserver.onNext(FacilitatorResponseResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void handleAdviserResponse(
      AdviserResponseRequest request, StreamObserver<AdviserResponseResponse> responseObserver) {
    pmsNodeExecutionService.handleAdviserResponse(
        request.getNodeExecutionId(), request.getNotifyId(), request.getAdviserResponse());

    responseObserver.onNext(AdviserResponseResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void handleEventError(EventErrorRequest request, StreamObserver<EventErrorResponse> responseObserver) {
    pmsNodeExecutionService.handleEventError(
        request.getEventType(), request.getEventNotifyId(), request.getFailureInfo());

    responseObserver.onNext(EventErrorResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
