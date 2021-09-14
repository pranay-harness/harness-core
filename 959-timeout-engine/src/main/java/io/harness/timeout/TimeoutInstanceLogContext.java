/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;

@OwnedBy(CDC)
public class TimeoutInstanceLogContext extends AutoLogContext {
  public static final String ID = LogKeyUtils.calculateLogKeyForId(TimeoutInstance.class);

  public TimeoutInstanceLogContext(String timeoutInstanceId, OverrideBehavior behavior) {
    super(ID, timeoutInstanceId, behavior);
  }
}
