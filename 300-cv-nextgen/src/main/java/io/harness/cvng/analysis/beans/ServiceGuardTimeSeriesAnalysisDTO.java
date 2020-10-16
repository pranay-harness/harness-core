package io.harness.cvng.analysis.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.NonFinal;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceGuardTimeSeriesAnalysisDTO {
  @NonFinal @Setter private String verificationTaskId;
  @NonFinal @Setter private Instant analysisStartTime;
  @NonFinal @Setter private Instant analysisEndTime;
  private Map<String, Double> overallMetricScores;
  private Map<String, Map<String, ServiceGuardTxnMetricAnalysisDataDTO>> txnMetricAnalysisData;
}
