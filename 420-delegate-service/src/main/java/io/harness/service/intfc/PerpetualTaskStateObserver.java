/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.intfc;

public interface PerpetualTaskStateObserver {
  void onPerpetualTaskAssigned(String accountId, String taskId, String delegateId);
}
