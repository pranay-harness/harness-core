package io.harness.ng.overview.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class TotalDeploymentInfo {
  private long count;
  private long production;
  private long nonProduction;
  private List<DeploymentDateAndCount> countList;
}
