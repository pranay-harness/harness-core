package io.harness.engine.executions.node;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.plan.AccumulateResponsesRequest;
import io.harness.pms.contracts.plan.AccumulateResponsesResponse;
import io.harness.pms.contracts.plan.AddExecutableResponseRequest;
import io.harness.pms.contracts.plan.AddExecutableResponseResponse;
import io.harness.pms.contracts.plan.AdviserResponseRequest;
import io.harness.pms.contracts.plan.AdviserResponseResponse;
import io.harness.pms.contracts.plan.FacilitatorResponseRequest;
import io.harness.pms.contracts.plan.FacilitatorResponseResponse;
import io.harness.pms.contracts.plan.HandleStepResponseRequest;
import io.harness.pms.contracts.plan.HandleStepResponseResponse;
import io.harness.pms.contracts.plan.NodeExecutionProtoServiceGrpc.NodeExecutionProtoServiceImplBase;
import io.harness.pms.contracts.plan.QueueNodeExecutionRequest;
import io.harness.pms.contracts.plan.QueueNodeExecutionResponse;
import io.harness.pms.contracts.plan.QueueTaskRequest;
import io.harness.pms.contracts.plan.QueueTaskResponse;
import io.harness.pms.contracts.plan.ResumeNodeExecutionRequest;
import io.harness.pms.contracts.plan.ResumeNodeExecutionResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class PmsNodeExecutionGrpcSevice extends NodeExecutionProtoServiceImplBase {
  @Inject private KryoSerializer kryoSerializer;
  @Inject private PmsNodeExecutionServiceImpl pmsNodeExecutionService;

  @Override
  public void queueNodeExecution(
      QueueNodeExecutionRequest request, StreamObserver<QueueNodeExecutionResponse> responseObserver) {
    pmsNodeExecutionService.queueNodeExecution(request.getNodeExecution());
    responseObserver.onNext(QueueNodeExecutionResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void queueTask(QueueTaskRequest request, StreamObserver<QueueTaskResponse> responseObserver) {
    String taskId = pmsNodeExecutionService.queueTask(
        request.getNodeExecutionId(), request.getSetupAbstractionsMap(), request.getTaskRequest());
    responseObserver.onNext(QueueTaskResponse.newBuilder().setTaskId(taskId).build());
    responseObserver.onCompleted();
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
    Map<String, ResponseData> response = new HashMap<>();
    if (EmptyPredicate.isNotEmpty(request.getResponseMap())) {
      request.getResponseMap().forEach(
          (k, v) -> response.put(k, (ResponseData) kryoSerializer.asInflatedObject(v.toByteArray())));
    }
    pmsNodeExecutionService.resumeNodeExecution(request.getNodeExecutionId(), response, request.getAsyncError());
    responseObserver.onNext(ResumeNodeExecutionResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void accumulateResponses(
      AccumulateResponsesRequest request, StreamObserver<AccumulateResponsesResponse> responseObserver) {
    Map<String, ResponseData> responseDataMap =
        pmsNodeExecutionService.accumulateResponses(request.getPlanExecutionId(), request.getNotifyId());
    Map<String, ByteString> response = new HashMap<>();
    if (EmptyPredicate.isNotEmpty(responseDataMap)) {
      responseDataMap.forEach((k, v) -> response.put(k, ByteString.copyFrom(kryoSerializer.asBytes(v))));
    }
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
}
