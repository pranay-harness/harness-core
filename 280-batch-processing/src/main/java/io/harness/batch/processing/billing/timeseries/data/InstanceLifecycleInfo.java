/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.billing.timeseries.data;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InstanceLifecycleInfo {
  String instanceId;
  Instant usageStartTime;
  Instant usageStopTime;
}
