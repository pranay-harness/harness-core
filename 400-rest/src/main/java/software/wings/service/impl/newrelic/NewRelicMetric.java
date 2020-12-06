package software.wings.service.impl.newrelic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 9/5/17.
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewRelicMetric {
  private String name;
}
