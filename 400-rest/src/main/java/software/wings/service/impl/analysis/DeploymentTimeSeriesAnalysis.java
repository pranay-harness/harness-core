/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import software.wings.metrics.RiskLevel;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "DeploymentTimeSeriesAnalysisKeys")
public class DeploymentTimeSeriesAnalysis {
  private String stateExecutionId;
  private String customThresholdRefId;
  private String baseLineExecutionId;
  private String message;
  private RiskLevel riskLevel;
  private int total;
  private List<NewRelicMetricAnalysis> metricAnalyses;
}
