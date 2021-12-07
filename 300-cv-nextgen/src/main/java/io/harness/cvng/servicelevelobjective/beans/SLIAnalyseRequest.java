package io.harness.cvng.servicelevelobjective.beans;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SLIAnalyseRequest {
  private Instant timeStamp;
  private double metricValue;
}
