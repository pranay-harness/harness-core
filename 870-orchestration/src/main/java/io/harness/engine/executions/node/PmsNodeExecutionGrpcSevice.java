package io.harness.engine.executions.node;

import io.harness.pms.plan.AddExecutableResponseRequest;
import io.harness.pms.plan.AddExecutableResponseResponse;
import io.harness.pms.plan.HandleStepResponseRequest;
import io.harness.pms.plan.HandleStepResponseResponse;
import io.harness.pms.plan.NodeExecutionProtoServiceGrpc.NodeExecutionProtoServiceImplBase;
import io.harness.pms.plan.QueueNodeExecutionRequest;
import io.harness.pms.plan.QueueNodeExecutionResponse;
import io.harness.pms.plan.QueueTaskRequest;
import io.harness.pms.plan.QueueTaskResponse;
import io.harness.pms.sdk.core.steps.io.StepResponseMapper;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.Task;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.stub.StreamObserver;

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
    String taskId = pmsNodeExecutionService.queueTask(request.getNodeExecutionId(), request.getTaskMode(),
        request.getSetupAbstractionsMap(), (Task) kryoSerializer.asInflatedObject(request.getTask().toByteArray()));
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
    pmsNodeExecutionService.handleStepResponse(
        request.getNodeExecutionId(), StepResponseMapper.fromStepResponseProto(request.getStepResponse()));
    responseObserver.onNext(HandleStepResponseResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
