package io.harness.accesscontrol.commons.iterators;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class IteratorConfig {
  boolean enabled;
  long targetIntervalInSeconds;
}
