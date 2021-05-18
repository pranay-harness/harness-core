package io.harness.ccm.commons.beans.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
public class TotalResourceUsage {
  // sum resource of all Pods
  double sumcpu;
  double summemory;
  // max resources for any Pod
  double maxcpu;
  double maxmemory;
}
