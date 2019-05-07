package software.wings.graphql.schema.type.instance.info;

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
public abstract class QLContainerInfo extends QLInstanceInfo {
  private String clusterName;

  public QLContainerInfo(String clusterName) {
    this.clusterName = clusterName;
  }
}
