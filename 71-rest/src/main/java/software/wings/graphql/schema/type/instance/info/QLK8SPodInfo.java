package software.wings.graphql.schema.type.instance.info;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QLK8SPodInfo extends QLContainerInfo {
  private String releaseName;
  private String podName;
  private String ip;
  private String namespace;
  private List<QLK8sContainerInfo> containers;

  @Builder
  public QLK8SPodInfo(String clusterName, String releaseName, String podName, String ip, String namespace,
      List<QLK8sContainerInfo> containers) {
    super(clusterName);
    this.releaseName = releaseName;
    this.podName = podName;
    this.ip = ip;
    this.namespace = namespace;
    this.containers = containers;
  }
}
