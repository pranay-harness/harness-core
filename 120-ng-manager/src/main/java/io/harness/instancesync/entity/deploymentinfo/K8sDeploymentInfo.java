package io.harness.instancesync.entity.deploymentinfo;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;

@Data
public class K8sDeploymentInfo extends DeploymentInfo {
  private String namespace;
  private String releaseName;
  private Integer releaseNumber;
  private Set<String> namespaces = new HashSet<>();
  private String blueGreenStageColor;
}
