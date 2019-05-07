package software.wings.graphql.schema.type.instance.info;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author rktummala on 01/30/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QLCodeDeployInstanceInfo extends QLAbstractEc2InstanceInfo {
  private String deploymentId;

  @Builder
  public QLCodeDeployInstanceInfo(String hostId, String hostName, String hostPublicDns, String deploymentId) {
    super(hostId, hostName, hostPublicDns);
    this.deploymentId = deploymentId;
  }
}
