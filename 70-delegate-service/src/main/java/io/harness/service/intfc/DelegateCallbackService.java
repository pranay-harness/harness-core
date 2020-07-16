package io.harness.service.intfc;

import io.harness.delegate.beans.DelegateTaskResponse;

public interface DelegateCallbackService {
  void publishTaskResponse(String delegateTaskId, DelegateTaskResponse response);
  void destroy();
}
