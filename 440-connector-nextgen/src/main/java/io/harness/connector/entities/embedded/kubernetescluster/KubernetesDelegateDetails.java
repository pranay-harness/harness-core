package io.harness.connector.entities.embedded.kubernetescluster;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("io.harness.connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails")
@OwnedBy(DX)
public class KubernetesDelegateDetails implements KubernetesCredential {
  Set<String> delegateSelectors;
}
