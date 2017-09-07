package software.wings.service.impl.newrelic;

import lombok.Data;

/**
 * Created by rsingh on 9/5/17.
 */
@Data
public class NewRelicErrors {
  private long errors_per_minute;
  private long error_count;
}
