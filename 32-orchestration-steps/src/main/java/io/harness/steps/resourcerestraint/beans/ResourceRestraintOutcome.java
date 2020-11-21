package io.harness.steps.resourcerestraint.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.Outcome;

import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class ResourceRestraintOutcome implements Outcome {
  String name;
  int capacity;
  String resourceUnit;
  int usage;
  int alreadyAcquiredPermits;
}
