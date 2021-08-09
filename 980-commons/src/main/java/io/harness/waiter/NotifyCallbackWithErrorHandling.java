package io.harness.waiter;

import io.harness.tasks.ResponseData;

import java.util.Map;
import java.util.function.Supplier;

public interface NotifyCallbackWithErrorHandling extends NotifyCallback {
  void notify(Map<String, Supplier<ResponseData> > response);
}