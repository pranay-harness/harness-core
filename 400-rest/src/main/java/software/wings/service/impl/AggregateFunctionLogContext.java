/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class AggregateFunctionLogContext extends AutoLogContext {
  public static final String ID = "aggregateFuncClass";

  public AggregateFunctionLogContext(String aggregateFuncClass, OverrideBehavior behavior) {
    super(ID, aggregateFuncClass, behavior);
  }
}
