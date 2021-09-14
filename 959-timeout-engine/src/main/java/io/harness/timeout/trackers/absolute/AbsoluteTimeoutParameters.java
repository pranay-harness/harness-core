/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.timeout.trackers.absolute;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.timeout.TimeoutParameters;

import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class AbsoluteTimeoutParameters implements TimeoutParameters {
  @Builder.Default long timeoutMillis = TimeoutParameters.DEFAULT_TIMEOUT_IN_MILLIS;
}
