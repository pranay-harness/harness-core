package software.wings.service.impl.analysis;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetricElement {
  private String name;
  private String host;
  private String groupName;
  private String tag;
  private long timestamp;
  @Builder.Default private Map<String, Double> values = new HashMap<>();

  public Map<String, Double> getValues() {
    if (values == null) {
      return new HashMap<>();
    }
    return values;
  }
}
