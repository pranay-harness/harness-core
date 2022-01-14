package io.harness.delegate.k8s.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
public class K8sHandlerConfig {
  private Kubectl client;
  private String releaseName;
  private List<KubernetesResource> resources;
  private KubernetesConfig kubernetesConfig;
  private String manifestFilesDirectory;
}