package software.wings.api;

import software.wings.helpers.ext.helm.response.HelmChartInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class K8sDeploymentInfo extends DeploymentInfo {
  private String namespace;
  private String releaseName;
  private Integer releaseNumber;
  private Set<String> namespaces = new HashSet<>();
  private HelmChartInfo helmChartInfo;
  private String blueGreenStageColor;

  @Builder
  public K8sDeploymentInfo(String namespace, String releaseName, Integer releaseNumber, Set<String> namespaces,
      HelmChartInfo helmChartInfo, String blueGreenStageColor) {
    this.namespace = namespace;
    this.releaseName = releaseName;
    this.releaseNumber = releaseNumber;
    this.namespaces = namespaces;
    this.helmChartInfo = helmChartInfo;
    this.blueGreenStageColor = blueGreenStageColor;
  }
}
