package io.harness.delegate.beans.ci.pod;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class CIK8ServicePodParams {
  @NonNull private String serviceName;
  @NonNull private Map<String, String> selectorMap;
  @NonNull private List<Integer> ports;
  @NonNull private CIK8PodParams<CIK8ContainerParams> cik8PodParams;
}
