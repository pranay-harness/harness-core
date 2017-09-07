package software.wings.service.impl.newrelic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Created by rsingh on 9/05/17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewRelicMetricDataResponse<T> {
  private NewRelicMetricData metric_data;
}
