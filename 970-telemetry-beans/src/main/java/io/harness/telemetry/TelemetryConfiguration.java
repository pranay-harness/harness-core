/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.telemetry;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

/**
 * Common config definition for telemetry module
 */
@OwnedBy(HarnessTeam.GTM)
public interface TelemetryConfiguration {
  boolean isEnabled();
  String getUrl();
  String getApiKey();
}
