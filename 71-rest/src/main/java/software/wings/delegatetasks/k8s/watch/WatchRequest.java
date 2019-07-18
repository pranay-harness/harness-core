package software.wings.delegatetasks.k8s.watch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

@Data
@Builder
@AllArgsConstructor
public class WatchRequest {
  private K8sClusterConfig k8sClusterConfig;
  private String k8sResourceKind;
  private String namespace;
}
