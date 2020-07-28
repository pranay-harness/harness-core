package io.harness.service.intfc;

public interface DelegateCallbackService {
  void publishSyncTaskResponse(String delegateTaskId, byte[] responseData);
  void publishAsyncTaskResponse(String delegateTaskId, byte[] responseData);
  void destroy();
}
