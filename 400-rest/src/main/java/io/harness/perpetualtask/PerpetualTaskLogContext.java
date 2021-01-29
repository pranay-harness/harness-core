package io.harness.perpetualtask;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.AutoLogContext;

@TargetModule(Module._420_DELEGATE_SERVICE)
public class PerpetualTaskLogContext extends AutoLogContext {
  public static final String ID = "perpetualTaskId";

  public PerpetualTaskLogContext(String perpetualTaskId, OverrideBehavior behavior) {
    super(ID, perpetualTaskId, behavior);
  }
}
