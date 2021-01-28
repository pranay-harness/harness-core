package software.wings.service.impl;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.Delegate;

@TargetModule(Module._420_DELEGATE_SERVICE)
public interface DelegateObserver {
  void onAdded(Delegate delegate);
  void onDisconnected(String accountId, String delegateId);
}
