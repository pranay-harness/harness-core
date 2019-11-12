package io.harness.waiter;

import io.harness.logging.AutoLogContext;

public class WaitInstanceLogContext extends AutoLogContext {
  public static final String ID = "WaitInstanceId";

  public WaitInstanceLogContext(String waitInstanceId, OverrideBehavior behavior) {
    super(ID, waitInstanceId, behavior);
  }
}
