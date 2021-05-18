package io.harness.cdng.Deployment;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class LastWorkloadInfo {
  private long startTime;
  private long endTime;
  private String deploymentType;
  private String status;
}
