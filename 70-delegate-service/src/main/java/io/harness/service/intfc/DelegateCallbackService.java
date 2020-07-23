package io.harness.service.intfc;

public interface DelegateCallbackService {
  void publishTaskResponse(String delegateTaskId, byte[] responseData);
  void destroy();
}
