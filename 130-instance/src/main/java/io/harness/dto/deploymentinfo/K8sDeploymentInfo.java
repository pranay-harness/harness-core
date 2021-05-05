package io.harness.dto.deploymentinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;

@Data
@OwnedBy(HarnessTeam.DX)
public class K8sDeploymentInfo extends DeploymentInfo {
  private String namespace;
  private String releaseName;
  private Integer releaseNumber;
  private Set<String> namespaces = new HashSet<>();
  private String blueGreenStageColor;
}
