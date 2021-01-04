package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class StaticLimitActionTypeLogContext extends AutoLogContext {
  public static final String ID = "staticLimitType";

  public StaticLimitActionTypeLogContext(String staticLimitType, OverrideBehavior behavior) {
    super(ID, staticLimitType, behavior);
  }
}
