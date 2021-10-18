package io.harness.mongo.iterator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
public class IteratorConfig {
  boolean enabled;
  int threadPoolCount;
  long targetIntervalInSeconds;
}
