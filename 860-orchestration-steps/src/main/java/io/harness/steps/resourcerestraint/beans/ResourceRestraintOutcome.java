package io.harness.steps.resourcerestraint.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@TypeAlias("resourceRestraintOutcome")
@JsonTypeName("resourceRestraintOutcome")
@RecasterAlias("io.harness.steps.resourcerestraint.beans.ResourceRestraintOutcome")
public class ResourceRestraintOutcome implements Outcome {
  String name;
  int capacity;
  String resourceUnit;
  int usage;
  int alreadyAcquiredPermits;
}
