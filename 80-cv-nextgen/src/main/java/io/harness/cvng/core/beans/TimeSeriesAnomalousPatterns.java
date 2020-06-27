package io.harness.cvng.core.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesAnomalousPatterns {
  private String cvConfigId;
  private List<TimeSeriesAnomalies> anomalies;
}
