package io.harness.cvng.core.beans;

import java.util.Map;

public class ServiceGuardMetricAnalysisDTO {
  private String cvConfigId;
  private long analysisMinute;
  Map<String, Double> overallMetricScores;
  Map<String, Map<String, ServiceGuardAnalysisDataDTO>> txnMetricAnalysisData;
}
