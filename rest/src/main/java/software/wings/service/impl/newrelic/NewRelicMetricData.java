package software.wings.service.impl.newrelic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Created by rsingh on 8/30/17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewRelicMetricData {
  private String from;
  private String to;

  private List<String> metrics_not_found;
  private List<String> metrics_found;

  private List<NewRelicMetricSlice> metrics;

  @Data
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
