/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.intfc;

public interface DelegateCallbackService {
  void publishSyncTaskResponse(String delegateTaskId, byte[] responseData);
  void publishAsyncTaskResponse(String delegateTaskId, byte[] responseData);
  void publishTaskProgressResponse(String delegateTaskId, String uuid, byte[] responseData);
  void destroy();
}
