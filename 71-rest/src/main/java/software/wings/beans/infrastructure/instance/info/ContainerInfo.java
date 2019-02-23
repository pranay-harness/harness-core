package software.wings.beans.infrastructure.instance.info;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Base class for container instance like docker
 * @author rktummala on 08/25/17
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class ContainerInfo extends InstanceInfo {
  private String clusterName;

  public ContainerInfo(String clusterName) {
    this.clusterName = clusterName;
  }
}
