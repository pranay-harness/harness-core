package software.wings.service.impl.newrelic;

import lombok.Data;

import java.util.List;

/**
 * Created by rsingh on 9/5/17.
 */
@Data
public class NewRelicMetricResponse {
  private List<NewRelicMetric> metrics;
}
