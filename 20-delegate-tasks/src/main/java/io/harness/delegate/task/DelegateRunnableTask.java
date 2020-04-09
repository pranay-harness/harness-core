package io.harness.delegate.task;

import io.harness.delegate.beans.ResponseData;

public interface DelegateRunnableTask extends Runnable {
  @Deprecated ResponseData run(Object[] parameters);
  ResponseData run(TaskParameters parameters);
}
