package io.harness.ng.cdOverview.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class DeploymentStatusInfoList {
  private List<String> planExecutionIdList;
  private List<String> namePipelineList;
  private List<Long> startTs;
  private List<Long> endTs;
  private List<String> deploymentStatus;
}
