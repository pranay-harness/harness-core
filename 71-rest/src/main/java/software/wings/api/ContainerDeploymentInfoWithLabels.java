package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.container.Label;
import software.wings.helpers.ext.helm.response.HelmChartInfo;

import java.util.List;

/**
 * This holds deploymentInfo of helm based deployments.
 * @author rktummala on 08/24/17
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class ContainerDeploymentInfoWithLabels extends BaseContainerDeploymentInfo {
  private List<Label> labels;
  private String newVersion;
  private String namespace;
  private HelmChartInfo helmChartInfo;

  @Builder
  public ContainerDeploymentInfoWithLabels(
      String clusterName, List<Label> labels, String newVersion, String namespace, HelmChartInfo helmChartInfo) {
    super(clusterName);
    this.labels = labels;
    this.newVersion = newVersion;
    this.namespace = namespace;
    this.helmChartInfo = helmChartInfo;
  }
}
