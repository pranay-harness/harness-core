package software.wings.service.impl;

import io.harness.delegate.beans.Delegate;

public interface DelegateObserver {
  void onAdded(Delegate delegate);
  void onDisconnected(String accountId, String delegateId);
  void onReconnected(String accountId, String delegateId);
}
