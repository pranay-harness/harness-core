package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TimeSeriesRiskData {
  int metricRisk;
  int longTermPattern;
  long lastSeenTime;
}
