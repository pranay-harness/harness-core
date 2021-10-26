package io.harness.grpc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.delegate.AbortExpireTaskRequest;
import io.harness.delegate.AbortTaskResponse;
import io.harness.delegate.DelegateClassicTaskRequest;
import io.harness.delegate.DelegateTaskGrpc;
import io.harness.delegate.ExecuteTaskResponse;
import io.harness.delegate.ExpireTaskResponse;
import io.harness.delegate.QueueTaskResponse;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateServiceClassicGrpcClient {
  private final DelegateTaskGrpc.DelegateTaskBlockingStub delegateTaskBlockingStub;
  private final KryoSerializer kryoSerializer;

  @Inject
  public DelegateServiceClassicGrpcClient(
      DelegateTaskGrpc.DelegateTaskBlockingStub blockingStub, KryoSerializer kryoSerializer) {
    this.delegateTaskBlockingStub = blockingStub;
    this.kryoSerializer = kryoSerializer;
  }

  public String queueTask(DelegateTask task) {
    ByteString.copyFrom(kryoSerializer.asDeflatedBytes(task));
    DelegateClassicTaskRequest delegateClassicTaskRequest =
        DelegateClassicTaskRequest.newBuilder()
            .setDelegateTaskKryo(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(task)))
            .build();
    final QueueTaskResponse queueTaskResponse = delegateTaskBlockingStub.queueTask(delegateClassicTaskRequest);
    return queueTaskResponse.getUuid();
  }

  public <T extends DelegateResponseData> T executeTask(DelegateTask task) {
    DelegateClassicTaskRequest delegateClassicTaskRequest =
        DelegateClassicTaskRequest.newBuilder()
            .setDelegateTaskKryo(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(task)))
            .build();

    final ExecuteTaskResponse executeTaskResponse = delegateTaskBlockingStub.executeTask(delegateClassicTaskRequest);
    return (T) kryoSerializer.asInflatedObject(executeTaskResponse.getDelegateTaskResponseKryo().toByteArray());
  }

  public DelegateTask abortTask(String accountId, String delegateTaskId) {
    final AbortTaskResponse abortTaskResponse = delegateTaskBlockingStub.abortTask(
        AbortExpireTaskRequest.newBuilder().setAccountId(accountId).setDelegateTaskId(delegateTaskId).build());
    return (DelegateTask) kryoSerializer.asInflatedObject(abortTaskResponse.getDelegateTaskKryo().toByteArray());
  }

  public String expireTask(String accountId, String delegateTaskId) {
    final ExpireTaskResponse expireTaskResponse = delegateTaskBlockingStub.expireTask(
        AbortExpireTaskRequest.newBuilder().setAccountId(accountId).setDelegateTaskId(delegateTaskId).build());
    return (String) kryoSerializer.asInflatedObject(expireTaskResponse.getMessageBytes().toByteArray());
  }
}