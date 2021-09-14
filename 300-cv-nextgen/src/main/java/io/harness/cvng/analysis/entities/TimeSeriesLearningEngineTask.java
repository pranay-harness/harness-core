/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.entities;

import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_SERVICE_GUARD_DATA_LENGTH;
import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_SERVICE_GUARD_WINDOW_SIZE;
import static io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType.SERVICE_GUARD_TIME_SERIES;

import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TimeSeriesLearningEngineTask extends LearningEngineTask {
  private String testDataUrl;
  private String previousAnalysisUrl;
  private String metricTemplateUrl;
  private String previousAnomaliesUrl;
  private String cumulativeSumsUrl;
  @Default private Set<String> keyTransactions = new HashSet<>();
  @Default private int tolerance = 1;
  @Default private int dataLength = TIMESERIES_SERVICE_GUARD_DATA_LENGTH;
  @Default private int windowSize = TIMESERIES_SERVICE_GUARD_WINDOW_SIZE;

  @Override
  public LearningEngineTaskType getType() {
    return SERVICE_GUARD_TIME_SERIES;
  }
}
