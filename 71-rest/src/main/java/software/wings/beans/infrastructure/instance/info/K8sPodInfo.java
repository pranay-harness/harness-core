package software.wings.beans.infrastructure.instance.info;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class K8sPodInfo extends ContainerInfo {
  private String releaseName;
  private String podName;
  private String ip;
  private String namespace;
  private List<K8sContainerInfo> containers;

  @Builder
  public K8sPodInfo(String clusterName, String releaseName, String podName, String ip, String namespace,
      List<K8sContainerInfo> containers) {
    super(clusterName);
    this.releaseName = releaseName;
    this.podName = podName;
    this.ip = ip;
    this.namespace = namespace;
    this.containers = containers;
  }
}
