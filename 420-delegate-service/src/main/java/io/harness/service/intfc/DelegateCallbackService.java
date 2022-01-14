package io.harness.service.intfc;

public interface DelegateCallbackService {
  void publishSyncTaskResponse(String delegateTaskId, byte[] responseData);
  void publishAsyncTaskResponse(String delegateTaskId, byte[] responseData);
  void publishTaskProgressResponse(String delegateTaskId, String uuid, byte[] responseData);
  void destroy();
}
