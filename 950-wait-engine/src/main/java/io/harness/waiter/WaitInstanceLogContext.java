/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.waiter;

import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;

public class WaitInstanceLogContext extends AutoLogContext {
  public static final String ID = LogKeyUtils.calculateLogKeyForId(WaitInstance.class);

  public WaitInstanceLogContext(String waitInstanceId, OverrideBehavior behavior) {
    super(ID, waitInstanceId, behavior);
  }
}
