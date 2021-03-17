package software.wings.service.intfc.perpetualtask;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._420_DELEGATE_SERVICE)
public interface PerpetualTaskCrudObserver {
  void onPerpetualTaskCreated();
  void onRebalanceRequired();
}
