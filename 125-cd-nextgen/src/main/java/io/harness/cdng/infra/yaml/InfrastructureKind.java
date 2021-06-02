package io.harness.cdng.infra.yaml;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDP)
public interface InfrastructureKind {
  String KUBERNETES_DIRECT = "KubernetesDirect";
  String KUBERNETES_GCP = "KubernetesGcp";
}
