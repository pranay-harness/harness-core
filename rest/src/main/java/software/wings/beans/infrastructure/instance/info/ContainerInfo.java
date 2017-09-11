package software.wings.beans.infrastructure.instance.info;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Base class for container instance like docker
 * @author rktummala on 08/25/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class ContainerInfo extends InstanceInfo {
  private String clusterName;
}
