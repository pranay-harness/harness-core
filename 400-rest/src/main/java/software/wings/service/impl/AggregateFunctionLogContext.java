package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class AggregateFunctionLogContext extends AutoLogContext {
  public static final String ID = "aggregateFuncClass";

  public AggregateFunctionLogContext(String aggregateFuncClass, OverrideBehavior behavior) {
    super(ID, aggregateFuncClass, behavior);
  }
}
