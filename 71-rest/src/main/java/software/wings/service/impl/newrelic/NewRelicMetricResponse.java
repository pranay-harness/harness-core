package software.wings.service.impl.newrelic;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Created by rsingh on 9/5/17.
 */
@Data
@Builder
public class NewRelicMetricResponse {
  private List<NewRelicMetric> metrics;
}
