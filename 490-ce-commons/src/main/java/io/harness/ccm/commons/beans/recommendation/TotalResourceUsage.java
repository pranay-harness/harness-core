package io.harness.ccm.commons.beans.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(CE)
public class TotalResourceUsage {
  // sum resource of ALL Pods
  double sumcpu;
  double summemory;
  // max resource of ANY Pod
  double maxcpu;
  double maxmemory;
}
