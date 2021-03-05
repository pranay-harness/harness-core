package software.wings.beans.container;

import software.wings.beans.DeploymentSpecificationYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class HelmChartSpecificationYaml extends DeploymentSpecificationYaml {
  private String chartUrl;
  private String chartName;
  private String chartVersion;

  @Builder
  public HelmChartSpecificationYaml(
      String type, String harnessApiVersion, String chartUrl, String chartName, String chartVersion) {
    super(type, harnessApiVersion);
    this.chartUrl = chartUrl;
    this.chartName = chartName;
    this.chartVersion = chartVersion;
  }
}
