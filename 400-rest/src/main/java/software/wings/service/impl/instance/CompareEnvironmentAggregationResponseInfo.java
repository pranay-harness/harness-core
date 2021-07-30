package software.wings.service.impl.instance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(DX)
public class CompareEnvironmentAggregationResponseInfo {
  private String serviceId;
  private String serviceName;
  private String count;
  Map<String, List<ServiceInfoResponseSummary>> envInfo;
}
