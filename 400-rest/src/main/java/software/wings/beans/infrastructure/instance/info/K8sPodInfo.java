package software.wings.beans.infrastructure.instance.info;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.helm.HelmChartInfo;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
public class K8sPodInfo extends ContainerInfo {
  private String releaseName;
  private String podName;
  private String ip;
  private String namespace;
  private List<K8sContainerInfo> containers;
  private HelmChartInfo helmChartInfo;
  private String blueGreenColor;

  @Builder
  public K8sPodInfo(String clusterName, String releaseName, String podName, String ip, String namespace,
      List<K8sContainerInfo> containers, HelmChartInfo helmChartInfo, String blueGreenColor) {
    super(clusterName);
    this.releaseName = releaseName;
    this.podName = podName;
    this.ip = ip;
    this.namespace = namespace;
    this.containers = containers;
    this.helmChartInfo = helmChartInfo;
    this.blueGreenColor = blueGreenColor;
  }
}
