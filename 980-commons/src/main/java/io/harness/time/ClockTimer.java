/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.time;

import java.time.Instant;

public class ClockTimer implements Timer {
  @Override
  public Instant now() {
    return Instant.now();
  }
}
