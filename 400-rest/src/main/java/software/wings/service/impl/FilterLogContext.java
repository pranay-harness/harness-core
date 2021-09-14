/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class FilterLogContext extends AutoLogContext {
  public static final String ID = "filterClass";

  public FilterLogContext(String filterClass, OverrideBehavior behavior) {
    super(ID, filterClass, behavior);
  }
}
