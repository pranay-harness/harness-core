/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import io.harness.delegate.beans.Delegate;

public interface DelegateObserver {
  void onAdded(Delegate delegate);
  void onDisconnected(String accountId, String delegateId);
  void onReconnected(String accountId, String delegateId);
}
