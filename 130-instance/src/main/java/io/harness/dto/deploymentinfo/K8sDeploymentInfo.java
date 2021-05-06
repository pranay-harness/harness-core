package io.harness.dto.deploymentinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.HashSet;
import java.util.Set;
import lombok.Builder;

@OwnedBy(HarnessTeam.DX)
public class K8sDeploymentInfo extends DeploymentInfo {
  private String namespace;
  private String releaseName;
  private Integer releaseNumber;
  private Set<String> namespaces = new HashSet<>();
  private String blueGreenStageColor;

  @Builder
  public K8sDeploymentInfo(
      String namespace, String releaseName, Integer releaseNumber, Set<String> namespaces, String blueGreenStageColor) {
    this.namespace = namespace;
    this.releaseName = releaseName;
    this.releaseNumber = releaseNumber;
    this.namespaces = namespaces;
    this.blueGreenStageColor = blueGreenStageColor;
  }
}
