package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This holds container deployment info.
 * @author rktummala on 08/24/17
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseContainerDeploymentInfo extends DeploymentInfo {
  private String clusterName;

  public BaseContainerDeploymentInfo(String clusterName) {
    this.clusterName = clusterName;
  }
}
