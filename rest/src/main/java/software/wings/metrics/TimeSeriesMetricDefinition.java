package software.wings.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * Created by sriram_parthasarathy on 11/29/17.
 */
@Builder
@Data
public class TimeSeriesMetricDefinition {
  private String metricName;
  private MetricType metricType;
  private Set<String> tags;
  private List<Threshold> customThresholds;

  @JsonProperty("metricType")
  public String getMetricTypeName() {
    return metricType.name();
  }

  @JsonProperty("thresholds")
  public List<Threshold> getThresholds() {
    return metricType.getThresholds();
  }
}
