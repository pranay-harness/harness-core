package io.harness.dto.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@OwnedBy(HarnessTeam.DX)
public class K8sPodInfo extends InstanceInfo {
  private String releaseName;
  private String podName;
  private String ip;
  private String namespace;
  private List<K8sContainerInfo> containers;
  private String blueGreenColor;
  private String clusterName;

  @Builder
  public K8sPodInfo(String releaseName, String podName, String ip, String namespace, List<K8sContainerInfo> containers,
      String blueGreenColor, String clusterName) {
    this.releaseName = releaseName;
    this.podName = podName;
    this.ip = ip;
    this.namespace = namespace;
    this.containers = containers;
    this.blueGreenColor = blueGreenColor;
    this.clusterName = clusterName;
  }
}
