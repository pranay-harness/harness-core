/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.activity.beans;

import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Builder
public class DeploymentActivityPopoverResultDTO {
  String tag;
  String serviceName;
  DeploymentPopoverSummary preProductionDeploymentSummary;
  DeploymentPopoverSummary productionDeploymentSummary;
  DeploymentPopoverSummary postDeploymentSummary;
  @Value
  @Builder
  public static class DeploymentPopoverSummary {
    int total;
    List<VerificationResult> verificationResults;
  }
  @Value
  @Builder
  public static class VerificationResult {
    String jobName;
    ActivityVerificationStatus status;
    Risk risk;
    Long remainingTimeMs;
    int progressPercentage;
    Long startTime;
  }
}
