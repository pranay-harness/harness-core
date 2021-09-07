package software.wings.service.impl.newrelic;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 9/5/17.
 */
@Data
@Builder
public class NewRelicMetricResponse {
  private List<NewRelicMetric> metrics;
}
