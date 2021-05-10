package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.container.ContainerInfo;
import io.harness.delegate.task.helm.HelmChartInfo;

import software.wings.beans.container.Label;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This holds deploymentInfo of helm based deployments.
 * @author rktummala on 08/24/17
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
public class ContainerDeploymentInfoWithLabels extends BaseContainerDeploymentInfo {
  private List<Label> labels;
  private String newVersion;
  private String namespace;
  private HelmChartInfo helmChartInfo;
  private List<ContainerInfo> containerInfoList;
  /*
   *   Helm Release to which the kubernetes pods belong to
   */
  private String releaseName;

  @Builder
  public ContainerDeploymentInfoWithLabels(String clusterName, List<Label> labels, String newVersion, String namespace,
      HelmChartInfo helmChartInfo, List<ContainerInfo> containerInfoList, String releaseName) {
    super(clusterName);
    this.labels = labels;
    this.newVersion = newVersion;
    this.namespace = namespace;
    this.helmChartInfo = helmChartInfo;
    this.containerInfoList = containerInfoList;
    this.releaseName = releaseName;
  }
}
