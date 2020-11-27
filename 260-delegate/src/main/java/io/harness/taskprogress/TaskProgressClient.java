package io.harness.taskprogress;

import io.harness.callback.DelegateCallbackToken;
import io.harness.delegate.AccountId;
import io.harness.delegate.TaskId;
import io.harness.delegate.beans.DelegateProgressData;
import io.harness.delegate.beans.taskprogress.ITaskProgressClient;
import io.harness.grpc.DelegateServiceGrpcLiteClient;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
public class TaskProgressClient implements ITaskProgressClient {
  @Inject private final DelegateServiceGrpcLiteClient delegateServiceGrpcLiteClient;
  @Inject private final KryoSerializer kryoSerializer;

  private final String accountId;
  private final String taskId;
  private final String delegateCallbackToken;

  @Override
  public boolean sendTaskProgressUpdate(DelegateProgressData delegateProgressData) {
    byte[] progressData = kryoSerializer.asDeflatedBytes(delegateProgressData);

    return delegateServiceGrpcLiteClient.sendTaskProgressUpdate(AccountId.newBuilder().setId(accountId).build(),
        TaskId.newBuilder().setId(taskId).build(),
        DelegateCallbackToken.newBuilder().setToken(delegateCallbackToken).build(), progressData);
  }
}
