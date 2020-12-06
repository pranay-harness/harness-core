package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class FilterLogContext extends AutoLogContext {
  public static final String ID = "filterClass";

  public FilterLogContext(String filterClass, OverrideBehavior behavior) {
    super(ID, filterClass, behavior);
  }
}
