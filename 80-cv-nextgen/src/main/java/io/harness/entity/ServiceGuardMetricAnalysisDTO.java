package io.harness.entity;

import java.util.Map;

public class ServiceGuardMetricAnalysisDTO {
  private String cvConfigId;
  private long analysisMinute;
  Map<String, Double> overallMetricScores;
  Map<String, Map<String, ServiceGuardAnalysisDataDTO>> txnMetricAnalysisData;
}
