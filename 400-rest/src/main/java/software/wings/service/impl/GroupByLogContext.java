/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class GroupByLogContext extends AutoLogContext {
  public static final String ID = "groupByClass";

  public GroupByLogContext(String groupByClass, OverrideBehavior behavior) {
    super(ID, groupByClass, behavior);
  }
}
