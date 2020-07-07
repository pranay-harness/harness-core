package io.harness;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;

import io.harness.delegate.NgDelegateTaskResponseServiceGrpc.NgDelegateTaskResponseServiceBlockingStub;
import io.harness.delegate.SendTaskResultRequest;
import io.harness.delegate.SendTaskResultResponse;
import io.harness.delegate.TaskId;
import io.harness.delegate.beans.ResponseData;
import io.harness.serializer.KryoSerializer;

import java.util.concurrent.TimeUnit;

@Singleton
public class NgManagerServiceDriver {
  private final NgDelegateTaskResponseServiceBlockingStub ngDelegateTaskServiceBlockingStub;
  private final KryoSerializer kryoSerializer;

  @Inject
  public NgManagerServiceDriver(
      NgDelegateTaskResponseServiceBlockingStub ngDelegateTaskServiceBlockingStub, KryoSerializer kryoSerializer) {
    this.ngDelegateTaskServiceBlockingStub = ngDelegateTaskServiceBlockingStub;
    this.kryoSerializer = kryoSerializer;
  }

  public boolean sendTaskResult(String taskId, ResponseData responseData) {
    SendTaskResultRequest resultRequest =
        SendTaskResultRequest.newBuilder()
            .setTaskId(TaskId.newBuilder().setId(taskId).build())
            .setResponseData(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(responseData)))
            .build();
    SendTaskResultResponse response =
        ngDelegateTaskServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).sendTaskResult(resultRequest);
    return response.getAcknowledgement();
  }
}
