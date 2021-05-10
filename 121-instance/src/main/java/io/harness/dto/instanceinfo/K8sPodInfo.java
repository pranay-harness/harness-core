package io.harness.dto.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@OwnedBy(HarnessTeam.DX)
public class K8sPodInfo extends ContainerInfo {
  private String releaseName;
  private String podName;
  private String ip;
  private String namespace;
  private List<K8sContainerInfo> containers;
  private String blueGreenColor;

  @Builder
  public K8sPodInfo(String clusterName, String releaseName, String podName, String ip, String namespace,
      List<K8sContainerInfo> containers, String blueGreenColor) {
    super(clusterName);
    this.releaseName = releaseName;
    this.podName = podName;
    this.ip = ip;
    this.namespace = namespace;
    this.containers = containers;
    this.blueGreenColor = blueGreenColor;
  }
}
