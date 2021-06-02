package io.harness.perpetualtask.ecs;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.time.Instant;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class EcsActiveInstancesCache {
  private Set<String> activeTaskArns;
  private Set<String> activeEc2InstanceIds;
  private Set<String> activeContainerInstanceArns;
  private Instant lastProcessedTimestamp;
  // instant till which we've collected metrics (truncated to hour)
  private Instant metricsCollectedTillHour;
}
