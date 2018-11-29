package io.harness.delegate.task;

import io.harness.delegate.task.protocol.ResponseData;
import io.harness.delegate.task.protocol.TaskParameters;

public interface DelegateRunnableTask extends Runnable {
  @Deprecated ResponseData run(Object[] parameters);
  ResponseData run(TaskParameters parameters);
}
