package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This holds controllers info about containers.
 * @author rktummala on 08/24/17
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class ContainerDeploymentInfoWithNames extends BaseContainerDeploymentInfo {
  /**
   * In case of ECS, this would be a list of taskDefinitionArns.
   * In case of Kubernetes, this would be a list of controllerNames.
   */
  private String containerSvcName;

  @Builder
  public ContainerDeploymentInfoWithNames(String clusterName, String containerSvcName) {
    super(clusterName);
    this.containerSvcName = containerSvcName;
  }
}
