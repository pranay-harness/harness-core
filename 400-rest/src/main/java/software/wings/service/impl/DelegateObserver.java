package software.wings.service.impl;

import software.wings.beans.Delegate;

public interface DelegateObserver {
  void onAdded(Delegate delegate);
  void onDisconnected(String accountId, String delegateId);
}
