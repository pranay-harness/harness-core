/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.newrelic;

import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.ExperimentStatus;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.sm.StateType;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ExperimentalMetricRecord {
  private String workflowExecutionId;
  private String stateExecutionId;
  private String cvConfigId;
  private Integer analysisMinute;

  private StateType stateType;

  private RiskLevel riskLevel;
  private RiskLevel experimentalRiskLevel;

  private TimeSeriesMlAnalysisType mlAnalysisType;

  private List<ExperimentalMetricAnalysis> metricAnalysis;

  private String baseLineExecutionId;
  private boolean mismatch;
  private ExperimentStatus experimentStatus;

  @Data
  @Builder
  public static class ExperimentalMetricAnalysis {
    private String metricName;
    private RiskLevel riskLevel;
    private RiskLevel experimentalRiskLevel;
    private List<ExperimentalMetricAnalysisValue> metricValues;
    private String displayName;
    private String fullMetricName;
    private String tag;
    private boolean mismatch;
  }

  @Data
  @Builder
  public static class ExperimentalMetricAnalysisValue {
    private String name;
    private String type;
    private String alertType;
    private RiskLevel riskLevel;
    private RiskLevel experimentalRiskLevel;
    private double testValue;
    private double controlValue;
    private List<ExperimentalMetricHostAnalysisValue> hostAnalysisValues;
    private boolean mismatch;
  }

  @Data
  @Builder
  public static class ExperimentalMetricHostAnalysisValue {
    private RiskLevel riskLevel;
    private RiskLevel experimentalRiskLevel;
    private String testHostName;
    private String controlHostName;
    private List<Double> testValues;
    private List<Double> controlValues;
    private List<Integer> anomalies;
    int testStartIndex;
    private boolean mismatch;
  }
}
