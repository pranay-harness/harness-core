package software.wings.metrics;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TimeSeriesCustomThresholdCriteria {
  TimeSeriesCustomThresholdActions actionToTake;
  int occurrences = 1;
}
