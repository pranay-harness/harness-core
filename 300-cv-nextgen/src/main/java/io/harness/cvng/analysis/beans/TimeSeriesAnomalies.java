package io.harness.cvng.analysis.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeSeriesAnomalies {
  private String transactionName;
  private String metricName;
  private List<Double> testData;
  private List<Long> anomalousTimestamps;
}
