package io.harness.perpetualtask;

import com.google.protobuf.Message;

public interface PerpetualTaskServiceClient<T> {
  String create(String accountId, T clientParams);

  boolean delete(String accountId, String taskId);

  Message getTaskParams(PerpetualTaskClientContext clientContext);
}
