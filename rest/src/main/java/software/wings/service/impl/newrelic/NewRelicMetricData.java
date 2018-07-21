package software.wings.service.impl.newrelic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Set;

/**
 * Created by rsingh on 8/30/17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class NewRelicMetricData {
  private String from;
  private String to;

  private Set<String> metrics_not_found;
  private Set<String> metrics_found;

  private Set<NewRelicMetricSlice> metrics;

  @Data
  @EqualsAndHashCode(exclude = "timeslices")
  public static class NewRelicMetricSlice {
    private String name;
    private List<NewRelicMetricTimeSlice> timeslices;
  }

  @Data
  public static class NewRelicMetricTimeSlice {
    private String from;
    private String to;

    private Object values;
  }
}
