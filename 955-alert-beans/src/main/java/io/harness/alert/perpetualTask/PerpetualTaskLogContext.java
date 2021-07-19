package io.harness.alert.perpetualTask;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.AutoLogContext;

public class PerpetualTaskLogContext extends AutoLogContext {
  public static final String ID = "perpetualTaskId";

  public PerpetualTaskLogContext(String perpetualTaskId, OverrideBehavior behavior) {
    super(ID, perpetualTaskId, behavior);
  }
}
