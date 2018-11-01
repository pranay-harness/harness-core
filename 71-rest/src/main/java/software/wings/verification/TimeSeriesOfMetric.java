package software.wings.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static final Logger logger = LoggerFactory.getLogger(TimeSeriesOfMetric.class);

  @Builder.Default int risk = -1;
  private String metricName;
  private List<TimeSeriesDataPoint> timeSeries;
  private List<TimeSeriesHighlight> highlights;
}
