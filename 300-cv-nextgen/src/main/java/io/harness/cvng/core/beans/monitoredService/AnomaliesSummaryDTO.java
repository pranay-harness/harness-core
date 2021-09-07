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
