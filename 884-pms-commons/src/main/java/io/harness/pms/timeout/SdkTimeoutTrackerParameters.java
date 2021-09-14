/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.timeout;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.timeout.TimeoutParameters;

@OwnedBy(HarnessTeam.PIPELINE)
public interface SdkTimeoutTrackerParameters {
  TimeoutParameters prepareTimeoutParameters();
}
