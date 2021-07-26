package software.wings.service.impl.instance;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class CompareEnvironmentAggregationResponseInfo {
    private String serviceId;
    Map<String, List<ServiceInfoSummary>> envInfo;

}
