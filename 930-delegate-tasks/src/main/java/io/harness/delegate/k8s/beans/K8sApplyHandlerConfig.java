package io.harness.delegate.k8s.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.model.KubernetesResource;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
public class K8sApplyHandlerConfig extends K8sHandlerConfig {
  private List<KubernetesResource> workloads;
  private List<KubernetesResource> customWorkloads;
}