package io.harness.ccm.commons.beans.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CE)
public class TotalResourceUsage {
  // sum resource of ALL Pods
  double sumcpu;
  double summemory;
  // max resource of ANY Pod
  double maxcpu;
  double maxmemory;
}
