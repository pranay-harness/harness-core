package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Created by sriram_parthasarathy on 10/17/17.
 */
@Data
@Builder
public class TimeSeriesMLMetricScores {
  private String metricName;
  private List<Double> scores;
}
