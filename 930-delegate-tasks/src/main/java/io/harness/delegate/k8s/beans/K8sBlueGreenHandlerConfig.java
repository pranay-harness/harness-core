package io.harness.delegate.k8s.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.k8s.PrePruningInfo;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.ReleaseHistory;

import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
public class K8sBlueGreenHandlerConfig extends K8sHandlerConfig {
  private ReleaseHistory releaseHistory;
  private Release currentRelease;
  private KubernetesResource managedWorkload;
  private KubernetesResource primaryService;
  private KubernetesResource stageService;
  private String primaryColor;
  private String stageColor;
  private PrePruningInfo prePruningInfo;
}
