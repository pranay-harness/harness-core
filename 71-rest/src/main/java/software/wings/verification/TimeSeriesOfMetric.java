package software.wings.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Vaibhav Tulsyan
 * 24/Oct/2018
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSeriesOfMetric {
  int risk;
  private String metricName;
  private List<TimeSeriesDataPoint> timeSeries;
  private List<TimeSeriesHighlight> highlights;
}
