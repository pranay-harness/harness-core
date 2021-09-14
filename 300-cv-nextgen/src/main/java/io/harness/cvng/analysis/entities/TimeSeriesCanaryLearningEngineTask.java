/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.entities;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TimeSeriesCanaryLearningEngineTask extends LearningEngineTask {
  private String postDeploymentDataUrl;
  private String preDeploymentDataUrl;
  private String metricTemplateUrl;
  private DeploymentVerificationTaskInfo deploymentVerificationTaskInfo;
  private int dataLength;
  private int tolerance;
  @Override
  public LearningEngineTaskType getType() {
    return LearningEngineTaskType.TIME_SERIES_CANARY;
  }

  @Value
  @Builder
  public static class DeploymentVerificationTaskInfo {
    private long deploymentStartTime;
    private Set<String> oldVersionHosts;
    private Set<String> newVersionHosts;
    private Integer newHostsTrafficSplitPercentage;
  }
}
