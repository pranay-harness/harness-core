package io.harness.cvng.core.beans;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
@Builder
public class TimeSeriesSampleDTO implements Comparable<TimeSeriesSampleDTO> {
  private String txnName;
  private String metricName;
  private double metricValue;
  private Long timestamp;

  @Override
  public int compareTo(@NotNull TimeSeriesSampleDTO o) {
    return this.timestamp.compareTo(o.timestamp);
  }
}
