/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.beans;

import io.harness.cvng.verificationjob.beans.AdditionalInfo;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Data
public abstract class CanaryBlueGreenAdditionalInfo extends AdditionalInfo {
  Set<HostSummaryInfo> primary;
  Set<HostSummaryInfo> canary;

  private String primaryInstancesLabel;
  private String canaryInstancesLabel;

  TrafficSplitPercentage trafficSplitPercentage;

  @Data
  @Builder
  @EqualsAndHashCode(of = "hostName")
  public static class HostSummaryInfo {
    String hostName;
    Risk risk;
    long anomalousMetricsCount;
    long anomalousLogClustersCount;
  }

  @Value
  @Builder
  public static class TrafficSplitPercentage {
    double preDeploymentPercentage;
    double postDeploymentPercentage;
  }

  public abstract void setFieldNames();
}
