package io.harness.steps.resourcerestraint.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.Outcome;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@TypeAlias("resourceRestraintOutcome")
public class ResourceRestraintOutcome implements Outcome {
  String name;
  int capacity;
  String resourceUnit;
  int usage;
  int alreadyAcquiredPermits;
}
