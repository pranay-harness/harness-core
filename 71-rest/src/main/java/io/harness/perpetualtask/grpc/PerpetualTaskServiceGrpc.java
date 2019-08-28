package io.harness.perpetualtask.grpc;

import com.google.inject.Inject;

import io.grpc.stub.StreamObserver;
import io.harness.perpetualtask.DelegateId;
import io.harness.perpetualtask.PerpetualTaskContext;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskIdList;
import io.harness.perpetualtask.PerpetualTaskService;

public class PerpetualTaskServiceGrpc
    extends io.harness.perpetualtask.PerpetualTaskServiceGrpc.PerpetualTaskServiceImplBase {
  private final PerpetualTaskService perpetualTaskService;

  @Inject
  public PerpetualTaskServiceGrpc(PerpetualTaskService perpetualTaskService) {
    this.perpetualTaskService = perpetualTaskService;
  }

  @Override
  public void listTaskIds(DelegateId request, StreamObserver<PerpetualTaskIdList> responseObserver) {
    PerpetualTaskIdList taskIdList =
        PerpetualTaskIdList.newBuilder().addAllTaskIds(perpetualTaskService.listTaskIds(request.getId())).build();
    responseObserver.onNext(taskIdList);
    responseObserver.onCompleted();
  }

  @Override
  public void getTaskContext(PerpetualTaskId request, StreamObserver<PerpetualTaskContext> responseObserver) {
    responseObserver.onNext(perpetualTaskService.getTaskContext(request.getId()));
    responseObserver.onCompleted();
  }
}
