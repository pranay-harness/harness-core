////#################################################
////Comment out the entire class since the code line include DelegateTask as input parameter,
////and this class suppose to move to upper layer to functioning, therefore comment out the code
////in order to compile successfully
////#################################################
//
// package io.harness.task.service.impl;
//
// import static io.harness.annotations.dev.HarnessTeam.DEL;
//
// import io.harness.annotations.dev.OwnedBy;
// import io.harness.beans.DelegateTask;
// import io.harness.delegate.beans.DelegateResponseData;
// import io.harness.serializer.KryoSerializer;
// import io.harness.task.service.DelegateClassicTaskRequest;
// import io.harness.task.service.DelegateTaskGrpc;
// import io.harness.task.service.ExecuteTaskReply;
// import io.harness.task.service.QueueTaskReply;
//
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.google.protobuf.ByteString;
// import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//@OwnedBy(DEL)
// public class DelegateTaskGrpcClient {
//  private final DelegateTaskGrpc.DelegateTaskBlockingStub delegateTaskBlockingStub;
//  private final KryoSerializer kryoSerializer;
//
//  public DelegateTaskGrpcClient(DelegateTaskGrpc.DelegateTaskBlockingStub blockingStub, KryoSerializer kryoSerializer)
//  {
//    this.delegateTaskBlockingStub = blockingStub;
//    this.kryoSerializer = kryoSerializer;
//  }
//
//  public String queueTask(DelegateTask task) {
//    ByteString.copyFrom(kryoSerializer.asDeflatedBytes(task));
//    DelegateClassicTaskRequest delegateClassicTaskRequest =
//        DelegateClassicTaskRequest.newBuilder()
//            .setDelegateTaskKryo(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(task)))
//            .build();
//    final QueueTaskReply queueTaskReply = delegateTaskBlockingStub.queueTask(delegateClassicTaskRequest);
//
//    return queueTaskReply.getUuid();
//  }
//
//  public <T extends DelegateResponseData> T executeTask(DelegateTask task) {
//    DelegateClassicTaskRequest delegateClassicTaskRequest =
//        DelegateClassicTaskRequest.newBuilder()
//            .setDelegateTaskKryo(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(task)))
//            .build();
//    ObjectMapper mapper = new ObjectMapper();
//
//    final ExecuteTaskReply executeTaskReply = delegateTaskBlockingStub.executeTask(delegateClassicTaskRequest);
//    DelegateExecuteTaskResponseData delegateExecuteTaskResponseData =
//        mapper.convertValue(executeTaskReply, DelegateExecuteTaskResponseData.class);
//    return (T) delegateExecuteTaskResponseData;
//  }
//}
