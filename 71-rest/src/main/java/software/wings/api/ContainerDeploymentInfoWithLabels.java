package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.container.Label;

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

  @Builder
  public ContainerDeploymentInfoWithLabels(String clusterName, List<Label> labels, String newVersion) {
    super(clusterName);
    this.labels = labels;
    this.newVersion = newVersion;
  }
}
