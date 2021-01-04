package io.harness.delegate.k8s.beans;

import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class K8sApplyHandlerConfig {
  private Kubectl client;
  private String releaseName;
  private List<KubernetesResource> resources;
  private List<KubernetesResource> workloads;
  private List<KubernetesResource> customWorkloads;
  private KubernetesConfig kubernetesConfig;
  private String manifestFilesDirectory;
}