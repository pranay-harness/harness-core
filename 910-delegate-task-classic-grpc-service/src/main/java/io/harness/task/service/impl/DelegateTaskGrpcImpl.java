////#################################################
////Comment out the entire class since the code line include DelegateTask as input parameter,
////and this class suppose to move to upper layer to functioning, therefore comment out the code
////in order to compile successfully
////#################################################
// package io.harness.task.service.impl;
//
// import static io.harness.annotations.dev.HarnessTeam.DEL;
//
// import io.harness.annotations.dev.OwnedBy;
// import io.harness.beans.DelegateTask;
// import io.harness.serializer.KryoSerializer;
// import io.harness.task.service.DelegateClassicTaskRequest;
// import io.harness.task.service.DelegateTaskGrpc;
// import io.harness.task.service.ExecuteTaskReply;
// import io.harness.task.service.QueueTaskReply;
//
// import software.wings.service.intfc.DelegateService;
//
// import io.grpc.stub.StreamObserver;
// import lombok.extern.slf4j.Slf4j;
//
//// this is server code, will eventually moved on to the server side
//@Slf4j
//@OwnedBy(DEL)
// public class DelegateTaskGrpcImpl extends DelegateTaskGrpc.DelegateTaskImplBase {
//  private KryoSerializer kryoSerializer;
//  private DelegateService delegateService;
//
//  @Override
//  public void queueTask(DelegateClassicTaskRequest request, StreamObserver<QueueTaskReply> responseObserver) {
//    try {
//      DelegateTask task = (DelegateTask) kryoSerializer.asInflatedObject(request.getDelegateTaskKryo().toByteArray());
//
//      if (task.getData().isParked()) {
//        delegateService.saveDelegateTask(task, DelegateTask.Status.PARKED);
//      } else {
//        if (task.getData().isAsync()) {
//          delegateService.queueTask(task);
//        } else {
//          delegateService.scheduleSyncTask(task);
//        }
//      }
//
//      responseObserver.onNext(QueueTaskReply.newBuilder().setUuid(task.getUuid()).build());
//      responseObserver.onCompleted();
//
//    } catch (Exception ex) {
//      log.error("Unexpected error occurred while processing queue task request.", ex);
//      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
//    }
//  }
//
//  @Override
//  public void executeTask(DelegateClassicTaskRequest request, StreamObserver<ExecuteTaskReply> responseObserver) {
//    try {
//      DelegateTask task = (DelegateTask) kryoSerializer.asInflatedObject(request.getDelegateTaskKryo().toByteArray());
//      delegateService.executeTask(task);
//      // tbd
//      responseObserver.onNext(ExecuteTaskReply.newBuilder().build());
//      responseObserver.onCompleted();
//    } catch (Exception ex) {
//      log.error("Unexpected error occurred while processing execute task request.", ex);
//      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
//    }
//  }
//}
