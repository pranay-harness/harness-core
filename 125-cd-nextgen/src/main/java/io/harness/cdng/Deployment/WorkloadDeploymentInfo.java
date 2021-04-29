package io.harness.cdng.Deployment;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class WorkloadDeploymentInfo {
  private String serviceName;
  private String lastExecuted;
  private String lastStatus;
  private long totalDeployments;
  private double percentSuccess;
  private double rateSuccess;
  private List<WorkloadDateCountInfo> workload;
}
