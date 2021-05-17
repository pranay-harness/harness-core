package io.harness.cdng.Deployment;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Value
@Builder
@OwnedBy(DX)
public class ExecutionDeploymentDetailInfo {
  long startTime;
  long endTime;
  long totalDeployments;
  double failureRate;
  double frequency;
  double failureRateChangeRate;
  double totalDeploymentsChangeRate;
  double frequencyChangeRate;
  List<ExecutionDeployment> executionDeploymentList;
}
