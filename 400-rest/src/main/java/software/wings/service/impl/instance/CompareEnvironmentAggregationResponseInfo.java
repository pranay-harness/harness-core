package software.wings.service.impl.instance;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompareEnvironmentAggregationResponseInfo {
  private String serviceId;
  private String serviceName;
  Map<String, List<ServiceInfoResponseSummary>> envInfo;
}
