package io.harness.cvng.analysis.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceGuardMetricAnalysisDTO {
  private String cvConfigId;
  private Instant analysisStartTime;
  private Instant analysisEndTime;
  private Map<String, Double> overallMetricScores;
  private Map<String, Map<String, ServiceGuardTxnMetricAnalysisDataDTO>> txnMetricAnalysisData;
}
