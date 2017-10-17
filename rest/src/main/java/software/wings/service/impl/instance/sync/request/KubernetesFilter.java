package software.wings.service.impl.instance.sync.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.KubernetesConfig;

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KubernetesFilter extends ContainerFilter {
  private KubernetesConfig kubernetesConfig;
  private Set<String> replicationControllerNameSet;
}
