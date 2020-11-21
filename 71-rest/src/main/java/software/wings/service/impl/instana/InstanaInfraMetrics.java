package software.wings.service.impl.instana;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InstanaInfraMetrics {
  private List<InstanaMetricItem> items;
}
