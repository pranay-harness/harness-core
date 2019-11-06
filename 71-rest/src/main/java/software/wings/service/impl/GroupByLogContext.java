package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class GroupByLogContext extends AutoLogContext {
  public static final String ID = "groupByClass";

  public GroupByLogContext(String groupByClass, OverrideBehavior behavior) {
    super(ID, groupByClass, behavior);
  }
}
