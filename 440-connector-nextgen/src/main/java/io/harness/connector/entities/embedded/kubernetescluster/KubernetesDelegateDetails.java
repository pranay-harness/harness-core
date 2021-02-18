package io.harness.connector.entities.embedded.kubernetescluster;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("io.harness.connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails")
public class KubernetesDelegateDetails implements KubernetesCredential {
  Set<String> delegateSelectors;
}
