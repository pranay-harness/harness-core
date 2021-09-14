/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans.monitoredService;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Value
public class AnomaliesSummaryDTO {
  long logsAnomalies;
  long timeSeriesAnomalies;
  long totalAnomalies;

  @Builder
  public AnomaliesSummaryDTO(long logsAnomalies, long timeSeriesAnomalies) {
    this.logsAnomalies = logsAnomalies;
    this.timeSeriesAnomalies = timeSeriesAnomalies;
    this.totalAnomalies = logsAnomalies + timeSeriesAnomalies;
  }
}
