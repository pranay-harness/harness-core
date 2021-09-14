/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.limits.configuration;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.limits.Action;

@OwnedBy(PL)
public class NoLimitConfiguredException extends RuntimeException {
  public NoLimitConfiguredException(Action action) {
    super("No limit configured. Action: " + action);
  }
}
