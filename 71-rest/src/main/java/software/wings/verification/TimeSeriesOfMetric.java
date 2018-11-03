package software.wings.verification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * @author Vaibhav Tulsyan
 * 24/Oct/2018
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSeriesOfMetric implements Comparable<TimeSeriesOfMetric> {
  private static final Logger logger = LoggerFactory.getLogger(TimeSeriesOfMetric.class);

  @Builder.Default int risk = -1;
  private String metricName;

  @JsonIgnore private SortedMap<Long, TimeSeriesDataPoint> timeSeries;
  private List<TimeSeriesHighlight> highlights;

  public Collection<TimeSeriesDataPoint> getTimeSeries() {
    return timeSeries.values();
  }

  @JsonIgnore
  public Map<Long, TimeSeriesDataPoint> getTimeSeriesMap() {
    return timeSeries;
  }

  @Override
  public int compareTo(@NotNull TimeSeriesOfMetric o) {
    if (o.risk != this.risk) {
      return o.risk - this.risk;
    }

    return this.metricName.compareTo(o.metricName);
  }
}
