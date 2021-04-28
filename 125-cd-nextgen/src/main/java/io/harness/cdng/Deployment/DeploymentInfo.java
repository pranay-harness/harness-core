package io.harness.cdng.Deployment;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class DeploymentInfo {
  private long count;
  private double rate;
  private List<DeploymentDateAndCount> countList;
}
