package io.harness.service.intfc;

import io.harness.beans.DelegateTask;
import io.harness.service.dto.RetryDelegate;

public interface DelegateTaskRetryObserver {
  RetryDelegate onPossibleRetry(RetryDelegate retryDelegate);
  void onTaskResponseProcessed(DelegateTask delegateTask, String delegateId);
}
