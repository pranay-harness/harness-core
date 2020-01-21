package software.wings.service.impl.instana;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InstanaInfraMetricRequest {
  private InstanaTimeFrame timeframe;
  private String query;
  private String plugin;
  private int rollup;
  private List<String> metrics;
}
