package software.wings.beans.ci.pod;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class CIK8ServicePodParams {
  @NonNull private String serviceName;
  @NonNull private Map<String, String> selectorMap;
  @NonNull private List<Integer> ports;
  @NonNull private CIK8PodParams<CIK8ContainerParams> cik8PodParams;
}
