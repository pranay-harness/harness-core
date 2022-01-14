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
