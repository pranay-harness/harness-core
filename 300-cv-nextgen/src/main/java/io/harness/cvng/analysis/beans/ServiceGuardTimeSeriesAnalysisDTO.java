/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.NonFinal;

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
