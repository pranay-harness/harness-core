package io.harness.cvng.core.dashboard.beans;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeatMapDTO implements Comparable<HeatMapDTO> {
  long startTime;
  long endTime;
  Double riskScore;

  @Override
  public int compareTo(HeatMapDTO o) {
    return Long.compare(this.startTime, o.startTime);
  }
}
