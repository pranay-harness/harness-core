package software.wings.service.impl.instana;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InstanaInfraMetricRequest {
  private InstanaTimeFrame timeframe;
  private String query;
  private String plugin;
  private int rollup;
  private List<String> metrics;
}
