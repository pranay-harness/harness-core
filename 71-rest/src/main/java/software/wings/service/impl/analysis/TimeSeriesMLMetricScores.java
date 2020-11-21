package software.wings.service.impl.analysis;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Created by sriram_parthasarathy on 10/17/17.
 */
@Data
@Builder
public class TimeSeriesMLMetricScores {
  private String metricName;
  private List<Double> scores;
}
