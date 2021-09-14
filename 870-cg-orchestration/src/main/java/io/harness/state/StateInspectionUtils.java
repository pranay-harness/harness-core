/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.state;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.time.Duration;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class StateInspectionUtils {
  public static final Duration TTL = Duration.ofDays(184);
}
