package io.harness.perpetualtask;

import io.harness.logging.AutoLogContext;

public class PerpetualTaskLogContext extends AutoLogContext {
  public static final String ID = "perpetualTaskId";

  public PerpetualTaskLogContext(String perpetualTaskId, OverrideBehavior behavior) {
    super(ID, perpetualTaskId, behavior);
  }
}
