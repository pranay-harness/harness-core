package software.wings.service.impl.newrelic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by rsingh on 8/30/17.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewRelicApdex {
  private float score;
  private long count;
  private float value;
  private float threshold;
  private float thresholdMin;
}
