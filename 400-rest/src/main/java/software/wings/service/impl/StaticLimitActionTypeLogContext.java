/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class StaticLimitActionTypeLogContext extends AutoLogContext {
  public static final String ID = "staticLimitType";

  public StaticLimitActionTypeLogContext(String staticLimitType, OverrideBehavior behavior) {
    super(ID, staticLimitType, behavior);
  }
}
