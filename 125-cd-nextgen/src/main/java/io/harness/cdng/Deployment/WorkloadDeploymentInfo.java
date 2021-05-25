package io.harness.cdng.Deployment;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkloadDeploymentInfo {
  String serviceName;
  String serviceId;
  LastWorkloadInfo lastExecuted;
  List<String> deploymentTypeList;
  long totalDeployments;
  double totalDeploymentChangeRate;
  double percentSuccess;
  double rateSuccess;
  double failureRate;
  double failureRateChangeRate;
  double frequency;
  double frequencyChangeRate;
  String lastPipelineExecutionId;
  List<WorkloadDateCountInfo> workload;
}
