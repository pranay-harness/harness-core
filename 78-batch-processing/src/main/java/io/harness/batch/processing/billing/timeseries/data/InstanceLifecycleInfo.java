package io.harness.batch.processing.billing.timeseries.data;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class InstanceLifecycleInfo {
  String instanceId;
  Instant usageStartTime;
  Instant usageStopTime;
}
