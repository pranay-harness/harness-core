package io.harness.connector.entities.embedded.kubernetescluster;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KubernetesDelegateDetails implements KubernetesCredential {
  String delegateName;
}
