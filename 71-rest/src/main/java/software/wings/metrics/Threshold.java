package software.wings.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by rsingh on 10/11/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Threshold {
  private ThresholdType thresholdType;
  private ThresholdComparisonType comparisonType;
  private double ml;
}
