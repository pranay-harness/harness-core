package io.harness.delegate.task;

import io.harness.delegate.beans.DelegateResponseData;

public interface DelegateRunnableTask extends Runnable {
  @Deprecated DelegateResponseData run(Object[] parameters);
  DelegateResponseData run(TaskParameters parameters);
}
