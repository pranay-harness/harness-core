package io.harness.perpetualtask.ecs;

import java.time.Instant;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EcsActiveInstancesCache {
  private Set<String> activeTaskArns;
  private Set<String> activeEc2InstanceIds;
  private Set<String> activeContainerInstanceArns;
  private Instant lastProcessedTimestamp;
  // instant till which we've collected metrics (truncated to hour)
  private Instant metricsCollectedTillHour;
}
