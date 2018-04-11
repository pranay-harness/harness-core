package software.wings.service.impl.newrelic;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Created by rsingh on 9/5/17.
 */
@Data
@EqualsAndHashCode(exclude = "values")
@Builder
public class NewRelicMetric {
  private String name;
  private List<String> values;
}
