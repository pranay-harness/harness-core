/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
